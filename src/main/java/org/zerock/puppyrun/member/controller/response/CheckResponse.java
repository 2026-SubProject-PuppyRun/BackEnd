package org.zerock.puppyrun.member.controller.response;

import lombok.Builder;

@Builder
public record CheckResponse(
        String object,
        boolean isExists
) {
}
