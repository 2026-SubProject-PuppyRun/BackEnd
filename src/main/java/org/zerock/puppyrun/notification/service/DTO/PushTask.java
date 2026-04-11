package org.zerock.puppyrun.notification.service.DTO;

import com.google.firebase.messaging.AndroidConfig;
import com.google.firebase.messaging.AndroidNotification;
import com.google.firebase.messaging.ApnsConfig;
import com.google.firebase.messaging.Aps;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import lombok.Builder;
import org.springframework.util.Assert;
import org.zerock.puppyrun.notification.entity.NotificationType;

public record PushTask(
        String fcmToken, // 토픽 발송일 경우 null
        String topic,    // 토큰 발송일 경우 null
        Message message
) {

    @Builder
    public PushTask(NotificationType type, String fcmToken, String topic, String title, String body) {
        // 표준 생성자로 위임
        this(fcmToken, topic, createMessage(type, fcmToken, topic, title, body));
    }

    /**
     * 알림 중요도에 따라 Message(Config 포함)를 동적으로 생성하는 헬퍼 메서드
     */
    private static Message createMessage(NotificationType type, String fcmToken, String topic, String title,
                                         String body) {

        // 공통 필수 값 검증
        Assert.notNull(type, "알림 타입은 필수 입니다.");
        Assert.hasText(title, "알림 제목은 필수 입니다.");
        Assert.hasText(body, "알림 본문은 필수 입니다.");

        // 토큰과 토픽 중 정확히 하나만 존재해야 함
        boolean hasToken = fcmToken != null && !fcmToken.isBlank();
        boolean hasTopic = topic != null && !topic.isBlank();
        Assert.isTrue(hasToken ^ hasTopic, "발송 대상(FCM 토큰 또는 토픽) 중 정확히 하나만 지정해야 합니다.");

        // 중요도에 따른 설정값 초기화
        AndroidConfig.Priority androidPriority;
        String apnsPriority;

        if (type.getPriority() == NotificationType.Priority.HIGH) {
            androidPriority = AndroidConfig.Priority.HIGH;
            apnsPriority = "10"; // iOS 수신 즉시 표시 (High)
        } else {
            androidPriority = AndroidConfig.Priority.NORMAL;
            apnsPriority = "5";  // iOS 배터리 고려하여 백그라운드에서 수신 (Normal)
        }

        // Message 빌더 공통 설정 조립
        Message.Builder messageBuilder = Message.builder()
                .setNotification(Notification.builder()
                        .setTitle(title)
                        .setBody(body)
                        .build())
                .putData("type", type.name())
                .putData("title", title)
                .putData("body", body)

                // (Android) 설정
                .setAndroidConfig(AndroidConfig.builder()
                        .setPriority(androidPriority)
//                        .setNotification(AndroidNotification.builder()
//                                .setChannelId("puppyrun")
//                                .build())
                        .build())

                // (iOS - APNs) 설정
                .setApnsConfig(ApnsConfig.builder()
                        .putHeader("apns-priority", apnsPriority)
                        .setAps(Aps.builder()
                                .setSound("default")
                                .build())
                        .build());

        // 발송 타겟 지정 (Token vs Topic)
        if (hasToken) {
            messageBuilder.setToken(fcmToken);
        } else {
            messageBuilder.setTopic(topic);
        }

        return messageBuilder.build();
    }
}
