package org.zerock.puppyrun.auth.controller.response;

import lombok.Builder;

@Builder
public record CheckResponse(
        String object,
        boolean isExists
) {
}
