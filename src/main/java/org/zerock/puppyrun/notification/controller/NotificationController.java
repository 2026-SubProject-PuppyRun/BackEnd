package org.zerock.puppyrun.notification.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.zerock.puppyrun.common.auth.security.UserPrincipal;
import org.zerock.puppyrun.notification.controller.request.FcmTokenUpdateRequest;
import org.zerock.puppyrun.notification.controller.request.NotificationAgreeRequest;
import org.zerock.puppyrun.notification.controller.request.NotificationToggleRequest;
import org.zerock.puppyrun.notification.controller.response.NotificationOptionsResponse;
import org.zerock.puppyrun.notification.service.NotificationCommandService;
import org.zerock.puppyrun.notification.service.NotificationQueryService;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationCommandService notificationCommandService;
    private final NotificationQueryService notificationQueryService;

    // 알림 설정 조회
    @GetMapping
    public ResponseEntity<NotificationOptionsResponse> getOptions(
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        NotificationOptionsResponse response = notificationQueryService.getOptions(userPrincipal);
        return ResponseEntity.ok(response);
    }

    // 전체 알림 설정 변경 (명시적 상태 전달)
    @PatchMapping("/options/global")
    public ResponseEntity<Void> toggleGlobal(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody NotificationAgreeRequest request
    ) {
        notificationCommandService.toggleGlobal(userPrincipal, request);
        return ResponseEntity.ok().build();
    }

    // 개별 알림 설정 변경
    @PatchMapping("/options")
    public ResponseEntity<Void> toggleIndividual(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody NotificationToggleRequest request
    ) {
        notificationCommandService.toggle(userPrincipal, request);
        return ResponseEntity.ok().build();
    }

    // 알림 동의 설정
    @PostMapping("/agree")
    public ResponseEntity<Void> agreeNotification(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody NotificationAgreeRequest request
    ) {
        notificationCommandService.saveNotification(userPrincipal, request);
        return ResponseEntity.ok().build();
    }

    // fcm token 등록
    @PostMapping("/fcm-token")
    public ResponseEntity<Void> updateFcmToken(
            @AuthenticationPrincipal UserPrincipal user,
            @Valid @RequestBody FcmTokenUpdateRequest request) {
        notificationCommandService.updateToken(user, request);
        return ResponseEntity.ok().build();
    }
}
