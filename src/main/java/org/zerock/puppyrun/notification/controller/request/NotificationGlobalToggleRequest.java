package org.zerock.puppyrun.notification.controller.request;

import jakarta.validation.constraints.NotNull;

public record NotificationGlobalToggleRequest(
        @NotNull(message = "전체 알람 동의는 필수입니다.")
        boolean isPushAgreed
) {
}
