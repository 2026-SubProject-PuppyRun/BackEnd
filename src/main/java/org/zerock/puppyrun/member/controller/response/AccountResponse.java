package org.zerock.puppyrun.member.controller.response;

import lombok.Builder;

@Builder
public record AccountResponse(
        String nickName,
        String email,
        String UserRole
) {
}
