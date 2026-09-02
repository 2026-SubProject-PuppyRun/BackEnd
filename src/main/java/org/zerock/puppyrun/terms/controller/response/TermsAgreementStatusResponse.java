package org.zerock.puppyrun.terms.controller.response;

import java.time.LocalDateTime;
import java.util.List;
import org.zerock.puppyrun.terms.entity.TermsType;

public record TermsAgreementStatusResponse(
        boolean agreementRequired,
        List<TermStatus> terms
    ) {
    public static TermsAgreementStatusResponse from(List<TermStatus> terms) {
        boolean agreementRequired = terms.stream()
                .anyMatch(term -> term.required() && !term.agreed());
        return new TermsAgreementStatusResponse(agreementRequired, terms);
    }

    public record TermStatus(
            TermsType type,
            String currentVersion,
            boolean required,
            boolean agreed,
            LocalDateTime agreedAt
    ) {
    }
}
