package org.zerock.puppyrun.terms.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.zerock.puppyrun.common.auth.security.UserPrincipal;
import org.zerock.puppyrun.member.entity.Member;
import org.zerock.puppyrun.member.exception.UserNotFoundException;
import org.zerock.puppyrun.member.repository.MemberRepository;
import org.zerock.puppyrun.terms.controller.request.TermsAgreementRequest;
import org.zerock.puppyrun.terms.entity.TermsAgreement;
import org.zerock.puppyrun.terms.entity.TermsType;
import org.zerock.puppyrun.terms.exception.TermsVersionChangedException;
import org.zerock.puppyrun.terms.repository.TermsAgreementRepository;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TermsAgreementCommandService {

    private final TermsAgreementRepository termsAgreementRepository;
    private final MemberRepository memberRepository;

    @Transactional
    public void agree(UserPrincipal userPrincipal, TermsAgreementRequest request) {
        validateCurrentVersions(request);

        Member member = memberRepository.findByIdForUpdate(userPrincipal.id())
                .orElseThrow(() -> new UserNotFoundException("존재하지 않는 회원입니다."));
        List<TermsAgreement> currentAgreements = termsAgreementRepository
                .findAllByMemberIdAndWithdrawnAtIsNull(userPrincipal.id());
        LocalDateTime agreedAt = LocalDateTime.now();

        List<TermsAgreement> newAgreements = new ArrayList<>();
        addIfMissing(
                newAgreements,
                currentAgreements,
                createAgreement(
                        member,
                        TermsType.SERVICE_TERMS,
                        agreedAt
                )
        );
        addIfMissing(
                newAgreements,
                currentAgreements,
                createAgreement(
                        member,
                        TermsType.PRIVACY_POLICY,
                        agreedAt
                )
        );

        if (!newAgreements.isEmpty()) {
            termsAgreementRepository.saveAll(newAgreements);
        }
    }

    private void validateCurrentVersions(TermsAgreementRequest request) {
        boolean serviceVersionChanged = !TermsType.SERVICE_TERMS.currentVersion()
                .equals(request.serviceTermsVersion());
        boolean privacyVersionChanged = !TermsType.PRIVACY_POLICY.currentVersion()
                .equals(request.privacyPolicyVersion());
        if (serviceVersionChanged || privacyVersionChanged) {
            throw new TermsVersionChangedException("현재 약관 버전을 다시 확인해주세요.");
        }
    }

    private void addIfMissing(
            List<TermsAgreement> newAgreements,
            List<TermsAgreement> currentAgreements,
            TermsAgreement candidate
    ) {
        boolean alreadyAgreed = currentAgreements.stream()
                .anyMatch(agreement -> agreement.getTermsType() == candidate.getTermsType()
                        && agreement.getTermsVersion().equals(candidate.getTermsVersion()));
        if (!alreadyAgreed) {
            newAgreements.add(candidate);
        }
    }

    private TermsAgreement createAgreement(
            Member member,
            TermsType termsType,
            LocalDateTime agreedAt
    ) {
        return TermsAgreement.builder()
                .member(member)
                .termsType(termsType)
                .termsVersion(termsType.currentVersion())
                .agreedAt(agreedAt)
                .build();
    }
}
