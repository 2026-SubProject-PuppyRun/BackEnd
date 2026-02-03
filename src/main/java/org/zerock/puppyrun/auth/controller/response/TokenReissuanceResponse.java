package org.zerock.puppyrun.auth.controller.response;

import lombok.Builder;

@Builder
public record TokenReissuanceResponse(
        String accessToken,
        String refreshToken
) {
}
