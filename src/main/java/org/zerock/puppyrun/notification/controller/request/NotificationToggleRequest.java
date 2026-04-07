package org.zerock.puppyrun.notification.controller.request;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record NotificationToggleRequest(
        @NotBlank(message = "알림 코드는 필수입니다.")
        String optionCode,
        @NotNull(message = "변경할 상태값(enabled)은 필수입니다.")
        Boolean enabled
) {
}
