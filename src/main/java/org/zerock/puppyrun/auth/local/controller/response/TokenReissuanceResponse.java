package org.zerock.puppyrun.auth.local.controller.response;

import lombok.Builder;

@Builder
public record TokenReissuanceResponse(
        String accessToken,
        String refreshToken
) {
}
