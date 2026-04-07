package org.zerock.puppyrun.notification.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.zerock.puppyrun.common.auth.security.UserPrincipal;
import org.zerock.puppyrun.notification.entity.NotificationType;
import org.zerock.puppyrun.notification.service.NotificationProcessor;

@RestController
@RequestMapping("/api/admin/notice")
@Slf4j
@RequiredArgsConstructor
public class AdminNoticeController {
    private final NotificationProcessor processor;

    @PostMapping("/send-push")
    public ResponseEntity<Void> sendNoticePush(
            @RequestParam String title,
            @RequestParam String body
    ) {

        // 전체 알림 발송
        processor.broadcast(NotificationType.NOTICE, title, body);

        return ResponseEntity.ok().build();
    }
}
