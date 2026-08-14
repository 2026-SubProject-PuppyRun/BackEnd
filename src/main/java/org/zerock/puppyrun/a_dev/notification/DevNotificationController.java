package org.zerock.puppyrun.a_dev.notification;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.zerock.puppyrun.a_dev.config.DevOnly;
import org.zerock.puppyrun.notification.client.NotificationEventClient;
import org.zerock.puppyrun.notification.entity.NotificationType;
import org.zerock.puppyrun.notification.client.DTO.TokenPushTask;
import org.zerock.puppyrun.notification.client.DTO.TopicPushTask;


@DevOnly
@RestController
@RequiredArgsConstructor
@RequestMapping("/test/notifications")
public class DevNotificationController {

    private final NotificationEventClient notificationEventClient;


    public record TokenNotificationRequest(
            String fcmToken,
            String notificationCode,
            String title,
            String body
    ) {
    }

    @PostMapping("/token")
    public ResponseEntity<Void> sendToToken(
            @RequestBody TokenNotificationRequest request
    ) {
        NotificationType type =
                NotificationType.fromCode(request.notificationCode());

        TokenPushTask task = new TokenPushTask(
                request.fcmToken(),
                type,
                request.title(),
                request.body()
        );

        notificationEventClient.sendMessagesInBulk(List.of(task));

        return ResponseEntity.accepted().build();
    }


    public record TopicNotificationRequest(
            String notificationCode,
            String title,
            String body
    ) {
    }

    @PostMapping("/topic")
    public ResponseEntity<Void> sendToTopic(
            @RequestBody TopicNotificationRequest request
    ) {
        NotificationType type =
                NotificationType.fromCode(request.notificationCode());

        TopicPushTask task = new TopicPushTask(
                type,
                request.title(),
                request.body()
        );

        notificationEventClient.sendMessage(task);

        return ResponseEntity.accepted().build();
    }
}
