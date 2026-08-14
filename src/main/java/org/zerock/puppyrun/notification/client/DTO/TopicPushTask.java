package org.zerock.puppyrun.notification.client.DTO;

import com.google.firebase.messaging.Message;
import org.zerock.puppyrun.notification.entity.NotificationType;

/**
 * 특정 알림 유형을 구독한 모든 기기에 공통 메시지를 보내는 작업입니다.
 */
public final class TopicPushTask extends AbstractPushTask {

    public TopicPushTask(NotificationType type, String title, String body) {
        super(type, title, body);
    }

    @Override
    public Message getMessage() {
        return createMessageBuilder().setTopic(target()).build();
    }

    @Override
    public String target() {
        return super.type().getCode();
    }
}
