package org.zerock.puppyrun.notification.service;

import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.zerock.puppyrun.notification.entity.NotificationType;
import org.zerock.puppyrun.notification.service.DTO.PushTask;
import org.zerock.puppyrun.notification.event.NotificationEventListener;
import org.zerock.puppyrun.notification.repository.DTO.EnabledNotifications;
import org.zerock.puppyrun.notification.repository.NotificationRepository;

@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationProcessor {
    // 의존성 주입
    private final NotificationRepository notificationRepository;
    private final NotificationEventListener notificationEventListener;

    private static final int CHUNK_SIZE = 1000;

    @Async("notificationTaskExecutor")
    public void broadcast(NotificationType type, Sender sender) {
        // Limit만 1000으로 걸어주는 용도의 Pageable
        Pageable limitOnly = PageRequest.of(0, CHUNK_SIZE);

        //옛날 날짜를 기준점
        LocalDateTime lastCreatedAt = null;
        // 알림 발송 로직이 시작되는 정확한 시간을 기록
        List<EnabledNotifications> memberSettings;

        do {
            // 마지막으로 본 시간 이후의 1000명을 조회
            memberSettings = notificationRepository.findNextMembers(lastCreatedAt, limitOnly, type);
            if (memberSettings.isEmpty()) {
                log.info("알림 가능한 멤버가 없습니다.");
                break; // 더 이상 데이터가 없으면 탈출
            }

            // 메세지를 다르게 만드는 분기
            List<PushTask> pushTasks = sender.setPushTasks(memberSettings);

            // 검색된 멤버 알림 처리
            notificationEventListener.sendMessagesInBulk(pushTasks);

            // 맨 마지막 사람의 가입일자를 다음 기준점(Cursor)으로 업데이트
            lastCreatedAt = memberSettings.getLast().createdAt();

        } while (memberSettings.size() == CHUNK_SIZE); // 딱 1000개를 꽉 채워서 가져왔다면 다음 데이터가 더 있을 확률이 높으므로 계속 반복
    }

    // 다수 토픽 알림
    @Async("notificationTaskExecutor")
    public void broadcast(NotificationType type, String title, String body) {

        // 공통 메세지 생성
        PushTask pushTask = PushTask.builder()
                .body(body)
                .title(title)
                .type(type)
                .topic(type.getCode())
                .build();

        notificationEventListener.sendTopicMessage(pushTask);

    }
}
