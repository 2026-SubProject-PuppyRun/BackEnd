package org.zerock.puppyrun.notification.controller.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record NotificationAgreeRequest(
        @NotNull(message = "수신 동의는 필수입니다.")
        boolean isPushAgreed,
        @NotBlank(message = "FCM 토큰은 필수값 입니다.")
        String fcmToken
) {
}
