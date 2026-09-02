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
import org.zerock.puppyrun.common.exception.InvalidValueException;
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
    @DisplayName("회원이 동의한 필수 약관과 마케팅 수신 동의 버전을 함께 저장한다")
    void agreeTerms() {
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
                        tuple(TermsType.PRIVACY_POLICY, TermsType.PRIVACY_POLICY.currentVersion()),
                        tuple(TermsType.LOCATION_INFORMATION, TermsType.LOCATION_INFORMATION.currentVersion()),
                        tuple(TermsType.MARKETING_AGREEMENT, TermsType.MARKETING_AGREEMENT.currentVersion())
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
        assertThat(termsAgreementRepository.findAllByMemberId(principal.id())).hasSize(4);
    }

    @Test
    @DisplayName("클라이언트가 지난 약관 버전을 제출하면 저장하지 않는다")
    void rejectOutdatedTermsVersion() {
        // given
        UserPrincipal principal = registerMember("outdated-agreement");
        TermsAgreementRequest request = request(
                TermsType.SERVICE_TERMS.currentVersion(),
                TermsType.PRIVACY_POLICY.currentVersion(),
                "0.9",
                false
        );

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
                .containsExactlyInAnyOrder(
                        TermsType.SERVICE_TERMS,
                        TermsType.PRIVACY_POLICY,
                        TermsType.LOCATION_INFORMATION,
                        TermsType.MARKETING_AGREEMENT
                );
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
                        tuple(TermsType.PRIVACY_POLICY, TermsType.PRIVACY_POLICY.currentVersion(), true),
                        tuple(TermsType.LOCATION_INFORMATION, TermsType.LOCATION_INFORMATION.currentVersion(), true),
                        tuple(TermsType.MARKETING_AGREEMENT, TermsType.MARKETING_AGREEMENT.currentVersion(), true)
                );
    }

    @Test
    @DisplayName("마케팅 수신에 동의하지 않아도 필수 약관 동의는 완료된다")
    void agreeRequiredTermsWithoutMarketingAgreement() {
        // given
        UserPrincipal principal = registerMember("without-marketing-agreement");
        TermsAgreementRequest request = currentVersionRequest(false);

        // when
        termsAgreementCommandService.agree(principal, request);
        TermsAgreementStatusResponse status = termsAgreementQueryService.getStatus(principal.id());

        // then
        assertThat(termsAgreementRepository.findAllByMemberId(principal.id()))
                .extracting(TermsAgreement::getTermsType)
                .containsExactlyInAnyOrder(
                        TermsType.SERVICE_TERMS,
                        TermsType.PRIVACY_POLICY,
                        TermsType.LOCATION_INFORMATION
                );
        assertThat(status.agreementRequired()).isFalse();
        assertThat(status.terms())
                .filteredOn(term -> term.type() == TermsType.MARKETING_AGREEMENT)
                .extracting(
                        TermsAgreementStatusResponse.TermStatus::required,
                        TermsAgreementStatusResponse.TermStatus::agreed
                )
                .containsExactly(tuple(false, false));
    }

    @Test
    @DisplayName("필수 약관에 동의하지 않으면 동의 이력을 저장하지 않는다")
    void rejectRequiredTermsDisagreement() {
        // given
        UserPrincipal principal = registerMember("required-terms-disagreement");
        TermsAgreementRequest request = new TermsAgreementRequest(
                new TermsAgreementRequest.ServiceTerms(
                        false,
                        TermsType.SERVICE_TERMS.currentVersion()
                ),
                new TermsAgreementRequest.PrivacyPolicy(
                        true,
                        TermsType.PRIVACY_POLICY.currentVersion()
                ),
                new TermsAgreementRequest.LocationInformation(
                        true,
                        TermsType.LOCATION_INFORMATION.currentVersion()
                ),
                new TermsAgreementRequest.MarketingAgreement(
                        false,
                        TermsType.MARKETING_AGREEMENT.currentVersion()
                )
        );

        // when & then
        assertThatThrownBy(() -> termsAgreementCommandService.agree(principal, request))
                .isInstanceOf(InvalidValueException.class)
                .hasMessage("필수 약관에 동의해야 합니다.");
        assertThat(termsAgreementRepository.findAllByMemberId(principal.id())).isEmpty();
    }

    private UserPrincipal registerMember(String identifier) {
        MemberDTO member = memberRegistrationService.registerLocalMember(
                identifier + "@test.com",
                identifier,
                "encoded-password"
        );
        return new UserPrincipal(member.id(), member.email(), UserRole.USER);
    }

    private TermsAgreementRequest request(
            String serviceTermsVersion,
            String privacyPolicyVersion,
            String locationInformationVersion,
            boolean marketingAgreed
    ) {
        return new TermsAgreementRequest(
                new TermsAgreementRequest.ServiceTerms(true, serviceTermsVersion),
                new TermsAgreementRequest.PrivacyPolicy(true, privacyPolicyVersion),
                new TermsAgreementRequest.LocationInformation(true, locationInformationVersion),
                new TermsAgreementRequest.MarketingAgreement(
                        marketingAgreed,
                        TermsType.MARKETING_AGREEMENT.currentVersion()
                )
        );
    }

    private TermsAgreementRequest currentVersionRequest() {
        return currentVersionRequest(true);
    }

    private TermsAgreementRequest currentVersionRequest(boolean marketingAgreed) {
        return request(
                TermsType.SERVICE_TERMS.currentVersion(),
                TermsType.PRIVACY_POLICY.currentVersion(),
                TermsType.LOCATION_INFORMATION.currentVersion(),
                marketingAgreed
        );
    }
}
