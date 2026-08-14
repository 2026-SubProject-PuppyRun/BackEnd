package org.zerock.puppyrun.notification.client.DTO;

import com.google.firebase.messaging.AndroidConfig;
import com.google.firebase.messaging.ApnsConfig;
import com.google.firebase.messaging.Aps;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import org.springframework.util.Assert;
import org.zerock.puppyrun.notification.entity.NotificationType;

/**
 * 모든 푸시 작업이 공유하는 알림 내용을 보유하는 추상 기반 클래스입니다.
 *
 * <p>하위 작업은 전송 대상과 최종 FCM 메시지 생성만 구현합니다.</p>
 */
public abstract class AbstractPushTask implements PushTask {

    private final NotificationType type;
    private final String title;
    private final String body;

    protected AbstractPushTask(NotificationType type, String title, String body) {
        Assert.notNull(type, "알림 타입은 필수입니다.");
        Assert.hasText(title, "알림 제목은 필수입니다.");
        Assert.hasText(body, "알림 본문은 필수입니다.");
        this.type = type;
        this.title = title;
        this.body = body;
    }


    @Override
    public final NotificationType type() {
        return type;
    }

    @Override
    public final String title() {
        return title;
    }

    @Override
    public final String body() {
        return body;
    }

    // 공통 메시지를 만든 뒤 전송 대상을 마지막에 다르게 지정하기 위해서 분리
    protected Message.Builder createMessageBuilder() {
        AndroidConfig.Priority androidPriority;
        String apnsPriority;

        if (this.type.getPriority() == NotificationType.Priority.HIGH) {
            androidPriority = AndroidConfig.Priority.HIGH;
            apnsPriority = "10";
        } else {
            androidPriority = AndroidConfig.Priority.NORMAL;
            apnsPriority = "5";
        }

        return Message.builder()
                .setNotification(Notification.builder()
                        .setTitle(this.title)
                        .setBody(this.body)
                        .build())
                .putData("type", this.type.name())
                .putData("title", this.title)
                .putData("body", this.body)

                // (Android) 설정
                .setAndroidConfig(AndroidConfig.builder()
                        .setPriority(androidPriority)
                        .build())

                // (iOS - APNs) 설정
                .setApnsConfig(ApnsConfig.builder()
                        .putHeader("apns-priority", apnsPriority)
                        .setAps(Aps.builder()
                                .setSound("default")
                                .build())
                        .build());
    }

}
