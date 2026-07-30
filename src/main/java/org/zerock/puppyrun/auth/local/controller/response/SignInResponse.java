package org.zerock.puppyrun.auth.local.controller.response;

import lombok.Builder;

@Builder
public record SignInResponse(
        String accessToken,
        String refreshToken
) {
}
