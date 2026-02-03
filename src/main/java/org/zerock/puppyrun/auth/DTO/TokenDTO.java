package org.zerock.puppyrun.auth.DTO;

import lombok.Builder;

@Builder
public record TokenDTO(
        String accessToken,
        String refreshToken
) {
}
