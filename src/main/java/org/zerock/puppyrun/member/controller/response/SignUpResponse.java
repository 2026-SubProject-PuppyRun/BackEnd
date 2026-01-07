package org.zerock.puppyrun.member.controller.response;

import lombok.Builder;

@Builder
public record SignUpResponse(
        String accessToken,
        String refreshToken,
        String email,
        String nickName
) {
}
