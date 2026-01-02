package org.zerock.puppyrun.member.DTO;

import lombok.Builder;

@Builder
public record TokenDTO(
        String accessToken,
        String refreshToken
) {
}
