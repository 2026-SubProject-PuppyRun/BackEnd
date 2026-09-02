package org.zerock.puppyrun.terms.controller.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record TermsAgreementRequest(
        @NotNull(message = "서비스 이용약관 동의 여부는 필수입니다.")
        @AssertTrue(message = "서비스 이용약관에 동의해야 합니다.")
        Boolean serviceTermsAgreed,

        @NotBlank(message = "서비스 이용약관 버전은 필수입니다.")
        @Size(max = 50, message = "서비스 이용약관 버전은 50자 이하여야 합니다.")
        String serviceTermsVersion,

        @NotNull(message = "개인정보 처리방침 동의 여부는 필수입니다.")
        @AssertTrue(message = "개인정보 처리방침에 동의해야 합니다.")
        Boolean privacyPolicyAgreed,

        @NotBlank(message = "개인정보 처리방침 버전은 필수입니다.")
        @Size(max = 50, message = "개인정보 처리방침 버전은 50자 이하여야 합니다.")
        String privacyPolicyVersion
) {
}
