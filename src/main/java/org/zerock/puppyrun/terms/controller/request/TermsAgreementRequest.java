package org.zerock.puppyrun.terms.controller.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record TermsAgreementRequest(
        @NotNull(message = "서비스 이용약관 동의 정보는 필수입니다.")
        @Valid
        ServiceTerms serviceTerms,

        @NotNull(message = "개인정보 처리방침 동의 정보는 필수입니다.")
        @Valid
        PrivacyPolicy privacyPolicy,

        @NotNull(message = "위치정보 이용약관 동의 정보는 필수입니다.")
        @Valid
        LocationInformation locationInformation,

        @NotNull(message = "마케팅 수신 동의 정보는 필수입니다.")
        @Valid
        MarketingAgreement marketingAgreement
) {
    public record ServiceTerms(
            @NotNull(message = "서비스 이용약관 동의 여부는 필수입니다.")
            @AssertTrue(message = "서비스 이용약관에 동의해야 합니다.")
            Boolean agreed,

            @NotBlank(message = "서비스 이용약관 버전은 필수입니다.")
            @Size(max = 50, message = "서비스 이용약관 버전은 50자 이하여야 합니다.")
            String version
    ) {
    }

    public record PrivacyPolicy(
            @NotNull(message = "개인정보 처리방침 동의 여부는 필수입니다.")
            @AssertTrue(message = "개인정보 처리방침에 동의해야 합니다.")
            Boolean agreed,

            @NotBlank(message = "개인정보 처리방침 버전은 필수입니다.")
            @Size(max = 50, message = "개인정보 처리방침 버전은 50자 이하여야 합니다.")
            String version
    ) {
    }

    public record LocationInformation(
            @NotNull(message = "위치정보 이용약관 동의 여부는 필수입니다.")
            @AssertTrue(message = "위치정보 이용약관에 동의해야 합니다.")
            Boolean agreed,

            @NotBlank(message = "위치정보 이용약관 버전은 필수입니다.")
            @Size(max = 50, message = "위치정보 이용약관 버전은 50자 이하여야 합니다.")
            String version
    ) {
    }

    public record MarketingAgreement(
            @NotNull(message = "마케팅 수신 동의 여부는 필수입니다.")
            Boolean agreed,

            @NotBlank(message = "마케팅 수신 동의 버전은 필수입니다.")
            @Size(max = 50, message = "마케팅 수신 동의 버전은 50자 이하여야 합니다.")
            String version
    ) {
    }
}
