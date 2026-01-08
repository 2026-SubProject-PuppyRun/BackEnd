package org.zerock.puppyrun.member.controller.response;

import lombok.Builder;

@Builder
public record TokenReissuanceResponse(
        String accessToken,
        String refreshToken
) {
}
