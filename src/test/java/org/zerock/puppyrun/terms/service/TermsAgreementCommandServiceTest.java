package org.zerock.puppyrun.terms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.groups.Tuple.tuple;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.zerock.puppyrun.a_config.TestContainerConfig;
import org.zerock.puppyrun.common.auth.security.UserPrincipal;
import org.zerock.puppyrun.member.DTO.MemberDTO;
import org.zerock.puppyrun.member.entity.Member;
import org.zerock.puppyrun.member.entity.UserRole;
import org.zerock.puppyrun.member.repository.MemberRepository;
import org.zerock.puppyrun.member.service.MemberRegistrationService;
import org.zerock.puppyrun.terms.controller.request.TermsAgreementRequest;
import org.zerock.puppyrun.terms.controller.response.TermsAgreementStatusResponse;
import org.zerock.puppyrun.terms.entity.TermsAgreement;
import org.zerock.puppyrun.terms.entity.TermsType;
import org.zerock.puppyrun.terms.exception.TermsVersionChangedException;
import org.zerock.puppyrun.terms.repository.TermsAgreementRepository;

class TermsAgreementCommandServiceTest extends TestContainerConfig {

    @Autowired
    private TermsAgreementCommandService termsAgreementCommandService;

    @Autowired
    private TermsAgreementQueryService termsAgreementQueryService;

    @Autowired
    private TermsAgreementRepository termsAgreementRepository;

    @Autowired
    private MemberRegistrationService memberRegistrationService;

    @Autowired
    private MemberRepository memberRepository;

    @Test
    @DisplayName("회원이 동의한 서비스 이용약관과 개인정보 처리방침 버전을 함께 저장한다")
    void agreeRequiredTerms() {
        // given
        UserPrincipal principal = registerMember("terms-agreement");
        TermsAgreementRequest request = currentVersionRequest();

        // when
        termsAgreementCommandService.agree(principal, request);

        // then
        List<TermsAgreement> agreements = termsAgreementRepository.findAllByMemberId(principal.id());
        assertThat(agreements)
                .extracting(TermsAgreement::getTermsType, TermsAgreement::getTermsVersion)
                .containsExactlyInAnyOrder(
                        tuple(TermsType.SERVICE_TERMS, TermsType.SERVICE_TERMS.currentVersion()),
                        tuple(TermsType.PRIVACY_POLICY, TermsType.PRIVACY_POLICY.currentVersion())
                );
        assertThat(agreements)
                .extracting(TermsAgreement::getAgreedAt)
                .doesNotContainNull()
                .allMatch(agreements.getFirst().getAgreedAt()::equals);
    }

    @Test
    @DisplayName("동일한 버전의 약관 동의를 다시 제출해도 중복 저장하지 않는다")
    void keepDuplicateAgreementIdempotent() {
        // given
        UserPrincipal principal = registerMember("duplicate-agreement");
        TermsAgreementRequest request = currentVersionRequest();
        termsAgreementCommandService.agree(principal, request);

        // when
        termsAgreementCommandService.agree(principal, request);

        // then
        assertThat(termsAgreementRepository.findAllByMemberId(principal.id())).hasSize(2);
    }

    @Test
    @DisplayName("클라이언트가 지난 약관 버전을 제출하면 저장하지 않는다")
    void rejectOutdatedTermsVersion() {
        // given
        UserPrincipal principal = registerMember("outdated-agreement");
        TermsAgreementRequest request = request("0.9", TermsType.PRIVACY_POLICY.currentVersion());

        // when & then
        assertThatThrownBy(() -> termsAgreementCommandService.agree(principal, request))
                .isInstanceOf(TermsVersionChangedException.class)
                .hasMessage("현재 약관 버전을 다시 확인해주세요.");
        assertThat(termsAgreementRepository.findAllByMemberId(principal.id())).isEmpty();
    }

    @Test
    @DisplayName("현재 버전 중 일부 약관만 동의되어 있으면 누락된 동의만 저장한다")
    void saveOnlyMissingAgreement() {
        // given
        UserPrincipal principal = registerMember("partial-agreement");
        Member member = memberRepository.findByIdOrThrow(principal.id());
        termsAgreementRepository.save(TermsAgreement.builder()
                .member(member)
                .termsType(TermsType.SERVICE_TERMS)
                .termsVersion(TermsType.SERVICE_TERMS.currentVersion())
                .agreedAt(LocalDateTime.of(2026, 9, 2, 10, 0))
                .build());

        // when
        termsAgreementCommandService.agree(principal, currentVersionRequest());

        // then
        assertThat(termsAgreementRepository.findAllByMemberId(principal.id()))
                .extracting(TermsAgreement::getTermsType)
                .containsExactlyInAnyOrder(TermsType.SERVICE_TERMS, TermsType.PRIVACY_POLICY);
    }

    @Test
    @DisplayName("현재 필수 약관 동의 여부를 회원 기준으로 확인한다")
    void getCurrentAgreementStatus() {
        // given
        UserPrincipal principal = registerMember("agreement-status");
        TermsAgreementStatusResponse beforeAgreement = termsAgreementQueryService.getStatus(principal.id());

        // when
        termsAgreementCommandService.agree(principal, currentVersionRequest());
        TermsAgreementStatusResponse afterAgreement = termsAgreementQueryService.getStatus(principal.id());

        // then
        assertThat(beforeAgreement.agreementRequired()).isTrue();
        assertThat(beforeAgreement.terms()).allMatch(term -> !term.agreed());
        assertThat(afterAgreement.agreementRequired()).isFalse();
        assertThat(afterAgreement.terms())
                .extracting(
                        TermsAgreementStatusResponse.TermStatus::type,
                        TermsAgreementStatusResponse.TermStatus::currentVersion,
                        TermsAgreementStatusResponse.TermStatus::agreed
                )
                .containsExactly(
                        tuple(TermsType.SERVICE_TERMS, TermsType.SERVICE_TERMS.currentVersion(), true),
                        tuple(TermsType.PRIVACY_POLICY, TermsType.PRIVACY_POLICY.currentVersion(), true)
                );
    }

    private UserPrincipal registerMember(String identifier) {
        MemberDTO member = memberRegistrationService.registerLocalMember(
                identifier + "@test.com",
                identifier,
                "encoded-password"
        );
        return new UserPrincipal(member.id(), member.email(), UserRole.USER);
    }

    private TermsAgreementRequest request(String serviceTermsVersion, String privacyPolicyVersion) {
        return new TermsAgreementRequest(
                true,
                serviceTermsVersion,
                true,
                privacyPolicyVersion
        );
    }

    private TermsAgreementRequest currentVersionRequest() {
        return request(
                TermsType.SERVICE_TERMS.currentVersion(),
                TermsType.PRIVACY_POLICY.currentVersion()
        );
    }
}
