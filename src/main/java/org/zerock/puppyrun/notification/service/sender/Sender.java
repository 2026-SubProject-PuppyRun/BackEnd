package org.zerock.puppyrun.notification.service.sender;

import java.util.List;

import org.zerock.puppyrun.notification.repository.DTO.EnabledNotifications;
import org.zerock.puppyrun.notification.client.DTO.PushTask;

/**
 * 알림 수신 대상자 정보를 회원별 토큰 전송 작업으로 변환하는 전략입니다.
 *
 * <p>알림 종류마다 다른 개인화 규칙은 이 인터페이스의 구현체로 분리합니다.</p>
 */
public interface Sender {

    /**
     * 수신 대상자별 알림 내용을 생성해 토큰 전송 작업 목록으로 반환합니다.
     *
     * @param memberSettings 알림 유형과 FCM 토큰을 포함한 수신 대상자 목록
     * @return 개인별 FCM 토큰과 알림 내용을 포함한 전송 작업 목록
     */
    List<PushTask> createPushTasks(List<EnabledNotifications> memberSettings);

}
