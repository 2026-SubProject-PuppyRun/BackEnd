package org.zerock.puppyrun.terms.service;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.zerock.puppyrun.terms.controller.response.TermsAgreementStatusResponse;
import org.zerock.puppyrun.terms.controller.response.TermsAgreementStatusResponse.TermStatus;
import org.zerock.puppyrun.terms.entity.TermsAgreement;
import org.zerock.puppyrun.terms.entity.TermsType;
import org.zerock.puppyrun.terms.repository.TermsAgreementRepository;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TermsAgreementQueryService {

    private final TermsAgreementRepository termsAgreementRepository;

    public TermsAgreementStatusResponse getStatus(UUID memberId) {
        List<TermsAgreement> agreements = termsAgreementRepository.findAllByMemberIdAndWithdrawnAtIsNull(memberId);

        return TermsAgreementStatusResponse.from(List.of(
                statusOf(TermsType.SERVICE_TERMS, agreements),
                statusOf(TermsType.PRIVACY_POLICY, agreements),
                statusOf(TermsType.LOCATION_INFORMATION, agreements),
                statusOf(TermsType.MARKETING_AGREEMENT, agreements)
        ));
    }

    private TermStatus statusOf(
            TermsType termsType,
            List<TermsAgreement> agreements
    ) {
        String currentVersion = termsType.currentVersion();
        Optional<TermsAgreement> currentAgreement = agreements.stream()
                .filter(agreement -> agreement.getTermsType() == termsType)
                .filter(agreement -> agreement.getTermsVersion().equals(currentVersion))
                .max(Comparator.comparing(TermsAgreement::getAgreedAt));

        return new TermStatus(
                termsType,
                currentVersion,
                termsType.required(),
                currentAgreement.isPresent(),
                currentAgreement.map(TermsAgreement::getAgreedAt).orElse(null)
        );
    }
}
