package org.zerock.puppyrun.notification.client.DTO;

import com.google.firebase.messaging.Message;
import org.zerock.puppyrun.notification.entity.NotificationType;

/**
 * 전송 대상별 푸시 메시지를 생성하는 공통 계약입니다.
 *
 * <p>구현체는 토큰 또는 토픽처럼 자신이 아는 전송 대상과 최종 FCM 메시지 생성을 담당합니다.</p>
 */
public interface PushTask {

    NotificationType type();

    String title();

    String body();

    Message getMessage();

    String target();
}
