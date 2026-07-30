package org.zerock.puppyrun.auth.oauth2.controller.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

/**
 * 소셜 로그인 인가 코드를 백엔드로 전달하는 요청입니다.
 *
 * @param authorizationCode 소셜 제공자가 발급한 인가 코드
 * @param redirectUrl 인가 요청에 사용한 리다이렉트 URL
 */
@Builder
public record OAuth2SignInRequest(
        @NotBlank(message = "소셜 로그인 인가 코드는 필수입니다.")
        String authorizationCode,
        @NotBlank(message = "리다이렉트 URL은 필수입니다.")
        String redirectUrl
) {
}
