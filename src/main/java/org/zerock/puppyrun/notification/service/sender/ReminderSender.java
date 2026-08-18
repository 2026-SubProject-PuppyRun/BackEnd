package org.zerock.puppyrun.notification.service.sender;


import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.zerock.puppyrun.notification.entity.NotificationType;
import org.zerock.puppyrun.notification.repository.DTO.EnabledNotifications;
import org.zerock.puppyrun.notification.client.DTO.PushTask;
import org.zerock.puppyrun.notification.client.DTO.TokenPushTask;
import org.zerock.puppyrun.tracking.DTO.DailyMemberStat;
import org.zerock.puppyrun.tracking.repository.TrackingRepository;

/**
 * 당일 산책 통계에 따라 회원별 산책 리마인더 문구를 생성합니다.
 *
 * <p>산책하지 않은 회원, 3km 이상 산책한 회원, 그 외 회원을 구분해
 * 각 FCM 토큰에 대응하는 개인화된 전송 작업을 반환합니다.</p>
 */
@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReminderSender implements Sender {
    private final TrackingRepository trackingRepository;

    /**
     * 알림 수신 대상자의 당일 산책 통계를 일괄 조회해 개인별 토큰 전송 작업을 생성합니다.
     *
     * @param memberSettings 알림 유형과 FCM 토큰을 포함한 수신 대상자 목록
     * @return 입력 대상자 순서에 대응하는 개인별 토큰 전송 작업 목록
     */
    @Override
    public List<PushTask> createPushTasks(List<EnabledNotifications> memberSettings) {
        // 오늘 날짜
        LocalDate today = LocalDate.now();

        List<UUID> memberIds = memberSettings.stream().map(EnabledNotifications::memberId).toList();
        List<DailyMemberStat> statList = trackingRepository.findMemberIdsByDate(memberIds, today, today);

        // 검색 속도를 위해 List를 Map 형태로 변환
        Map<UUID, DailyMemberStat> statMap = statList.stream()
                .collect(Collectors.toMap(DailyMemberStat::memberId, stat -> stat));

        return memberSettings.stream()
                .<PushTask>map(member -> {
                    DailyMemberStat stat = statMap.get(member.memberId());
                    return createTask(member.type(), member.fcmToken(), stat);
                })
                .toList();
    }

    private TokenPushTask createTask(NotificationType type, String fcmToken, DailyMemberStat stat) {
        String message;
        if (stat == null) {
            // Map에 없으면 산책을 아예 안 한 사람
            message = "아직 산책 전이신가요? 강아지가 문 앞을 서성이고 있어요! 🦮";
        } else if (stat.totalDistance() >= 3000) { // 3km 이상
            message = "와우! 무려 " + (stat.totalDistance() / 1000.0) + "km나 걸으셨네요! 오늘 꿀잠 예약입니다! 🔥";
        } else { // 3km 미만
            message = "오늘도 잊지 않고 산책 완료! 훌륭한 보호자이십니다 🐾";
        }

        return new TokenPushTask(
                fcmToken,
                type,
                "오늘도 산책할 시간이에요!",
                message
        );
    }
}
