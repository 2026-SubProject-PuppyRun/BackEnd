package org.zerock.puppyrun.notification.client.DTO;

import com.google.firebase.messaging.Message;
import java.util.Optional;
import org.springframework.util.Assert;
import org.zerock.puppyrun.notification.entity.NotificationType;

/**
 * 특정 기기의 FCM 토큰으로 개인 메시지를 보내는 작업입니다.
 */
public final class TokenPushTask extends AbstractPushTask {

    private final String fcmToken;

    public TokenPushTask(String fcmToken, NotificationType type, String title, String body) {
        super(type, title, body);
        Assert.hasText(fcmToken, "FCM 토큰은 필수입니다.");
        this.fcmToken = fcmToken;
    }

    @Override
    public Message getMessage() {
        return createMessageBuilder().setToken(target()).build();
    }

    @Override
    public String target() {
        return fcmToken;
    }
}
