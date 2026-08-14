package org.zerock.puppyrun.notification.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.zerock.puppyrun.notification.entity.NotificationType;
import org.zerock.puppyrun.notification.client.NotificationEventClient;
import org.zerock.puppyrun.notification.repository.DTO.EnabledNotifications;
import org.zerock.puppyrun.notification.repository.NotificationRepository;
import org.zerock.puppyrun.notification.client.DTO.PushTask;
import org.zerock.puppyrun.notification.client.DTO.TopicPushTask;
import org.zerock.puppyrun.notification.service.sender.Sender;

/**
 * 대상을 조회하고 전송 작업을 생성해 FCM 발송 클라이언트에 전달하는 알림 처리 서비스입니다.
 *
 * <p>개인화 알림은 회원별 토큰 작업으로 나누어 청크 단위로 처리하고,
 * 공통 알림은 알림 타입에 대응하는 하나의 토픽으로 전송합니다.</p>
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationProcessor {
    // 의존성 주입
    private final NotificationRepository notificationRepository;
    private final NotificationEventClient notificationEventClient;

    private static final int CHUNK_SIZE = 1000;

    /**
     * 알림 수신 대상자를 순차 조회해 회원별로 개인화된 토큰 메시지를 발송합니다.
     *
     * <p>대상자는 생성 시각과 회원 ID로 구성된 커서를 기준으로 페이지 크기보다 한 명 더 조회해
     * 다음 데이터 존재 여부를 판단하고, 실제 메시지 내용은 전달받은 {@link Sender} 구현체가 생성합니다.</p>
     *
     * @param type   조회할 알림 유형
     * @param sender 회원별 토큰 메시지 생성 전략
     */
    @Async("notificationTaskExecutor")
    public void broadcast(NotificationType type, Sender sender) {
        // Limit만 1000으로 걸어주는 용도의 Pageable
        Pageable limitOnly = PageRequest.of(0, CHUNK_SIZE);

        LocalDateTime lastCreatedAt = null;
        UUID lastMemberId = null;
        List<EnabledNotifications> memberSettings;

        do {
            memberSettings = notificationRepository.findNextMembers(
                    lastCreatedAt,
                    lastMemberId,
                    limitOnly,
                    type
            );
            if (memberSettings.isEmpty()) {
                break; // 더 이상 데이터가 없으면 탈출
            }
            // 메세지를 다르게 만드는 분기
            List<PushTask> pushTasks = sender.createPushTasks(memberSettings);

            // 검색된 멤버 알림 처리
            notificationEventClient.sendMessagesInBulk(pushTasks);

            EnabledNotifications lastMember = memberSettings.getLast();
            lastCreatedAt = lastMember.createdAt();
            lastMemberId = lastMember.memberId();

            // CHUNK_SIZE 보다 크면 다시 조회
        } while (memberSettings.size() > CHUNK_SIZE);
    }

    /**
     * 동일한 내용을 해당 알림 유형의 FCM 토픽 구독자에게 발송합니다.
     *
     * @param type  토픽을 결정할 알림 유형
     * @param title 알림 제목
     * @param body  알림 본문
     */
    @Async("notificationTaskExecutor")
    public void broadcast(NotificationType type, String title, String body) {

        // 공통 메세지 생성
        PushTask pushTask = new TopicPushTask(type, title, body);

        notificationEventClient.sendMessage(pushTask);

    }
}
