package org.zerock.puppyrun.notification.service;


import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.zerock.puppyrun.notification.entity.NotificationType;
import org.zerock.puppyrun.notification.service.DTO.PushTask;
import org.zerock.puppyrun.notification.repository.DTO.EnabledNotifications;
import org.zerock.puppyrun.tracking.DTO.DailyMemberStat;
import org.zerock.puppyrun.tracking.repository.TrackingRepository;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReminderSender implements Sender {
    private final TrackingRepository trackingRepository;

    @Override
    public List<PushTask> setPushTasks(NotificationType type, List<EnabledNotifications> memberSettings) {
        // 오늘 날짜
        LocalDate today = LocalDate.now();

        List<UUID> memberIds = memberSettings.stream().map(EnabledNotifications::memberId).toList();
        List<DailyMemberStat> statList = trackingRepository.findMemberIdsByDate(memberIds, today, today);

        // 검색 속도를 위해 List를 Map 형태로 변환
        Map<UUID, DailyMemberStat> statMap = statList.stream()
                .collect(Collectors.toMap(DailyMemberStat::memberId, stat -> stat));

        return memberSettings.stream()
                .map(member -> {
                    DailyMemberStat stat = statMap.get(member.memberId());
                    return setTask(type, member.fcmToken(), stat);
                })
                .toList();
    }

    private PushTask setTask(NotificationType type, String fcmToken, DailyMemberStat stat) {
        String message;
        if (stat == null) {
            // Map에 없으면 산책을 아예 안 한 사람
            message = "아직 산책 전이신가요? 강아지가 문 앞을 서성이고 있어요! 🦮";
        } else if (stat.totalDistance() >= 3000) { // 3km 이상
            message = "와우! 무려 " + (stat.totalDistance() / 1000.0) + "km나 걸으셨네요! 오늘 꿀잠 예약입니다! 🔥";
        } else { // 3km 미만
            message = "오늘도 잊지 않고 산책 완료! 훌륭한 보호자이십니다 🐾";
        }

        return PushTask.builder()
                .fcmToken(fcmToken)
                .type(type)
                .title("오늘도 산책할 시간이에요!")
                .body(message)
                .build();
    }
}
