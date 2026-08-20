package org.zerock.puppyrun.auth.oauth2.controller.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

/**
 * 소셜 제공자가 발급한 액세스 토큰을 백엔드로 전달하는 요청입니다.
 *
 * @param providerAccessToken 소셜 제공자가 발급한 액세스 토큰
 */
@Builder
public record OAuth2SignInRequest(
        @NotBlank(message = "소셜 액세스 토큰은 필수입니다.")
        String providerAccessToken
) {
}
