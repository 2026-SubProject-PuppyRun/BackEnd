package org.zerock.puppyrun.notification.service.sender;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.zerock.puppyrun.notification.client.DTO.PushTask;
import org.zerock.puppyrun.notification.entity.UserSelectedWalkingTime;
import org.zerock.puppyrun.notification.repository.DTO.EnabledNotifications;
import org.zerock.puppyrun.notification.repository.UserSelectedWalkingTimeRepository;

/**
 * 회원이 직접 저장한 산책 시간을 기준으로 추천 시간을 정하는 Sender입니다.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class UserSelectedWeatherRecommendSender implements Sender {

    private final UserSelectedWalkingTimeRepository userSelectedWalkingTimeRepository;
    private final WeatherRecommendationMessageComposer weatherRecommendationMessageComposer;


    @Override
    public List<PushTask> createPushTasks(List<EnabledNotifications> memberSettings) {
        if (memberSettings == null || memberSettings.isEmpty()) {
            return List.of();
        }

        LocalDateTime referenceTime = LocalDateTime.now(java.time.ZoneId.of("Asia/Seoul"));
        // 사용자 선택값은 회원당 한 건으로 저장되어 있으므로 현재 청크의 회원 ID로 일괄 조회한다.
        List<UUID> memberIds = memberSettings.stream().map(EnabledNotifications::memberId).toList();
        List<WeatherRecommendationTarget> targets = userSelectedWalkingTimeRepository
                .findAllByMemberIdIn(memberIds)
                .stream()
                // 선택 시간 또는 위치를 저장하지 않은 회원은 조회 결과에 없으므로 자동으로 제외된다.
                .map(this::toTarget)
                .toList();
        log.info("event=user_selected_weather_recommendation_targets_resolved, recipientCount={}, targetCount={}",
                memberSettings.size(), targets.size());
        // 사용자 선택값의 출처와 무관하게 날씨, 메시지 생성 규칙은 Composer와 공유한다.
        return weatherRecommendationMessageComposer.createPushTasks(memberSettings, targets, referenceTime);
    }

    private WeatherRecommendationTarget toTarget(UserSelectedWalkingTime selection) {
        return new WeatherRecommendationTarget(
                selection.getMember().getId(),
                selection.getLatitude(),
                selection.getLongitude(),
                selection.getTime()
        );
    }
}
