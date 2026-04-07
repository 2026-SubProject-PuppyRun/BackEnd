package org.zerock.puppyrun.notification.controller.request;


import jakarta.validation.constraints.NotBlank;

public record FcmTokenUpdateRequest(
        @NotBlank(message = "FCM 토큰은 필수입니다.")
        String fcmToken
) {
}
