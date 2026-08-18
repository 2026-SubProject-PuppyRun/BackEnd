package org.zerock.puppyrun.notification.service.sender;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.zerock.puppyrun.notification.client.DTO.PushTask;
import org.zerock.puppyrun.notification.entity.WalkingPreference;
import org.zerock.puppyrun.notification.repository.DTO.EnabledNotifications;
import org.zerock.puppyrun.notification.repository.WalkingPreferenceRepository;

/**
 * 최근 7일 산책 스냅샷을 기준으로 추천 시간을 정하는 Sender입니다.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class SnapshotWeatherRecommendSender implements Sender {

    private static final int PREFERENCE_VALID_DAYS = 7;
    private static final java.time.ZoneId KOREA_ZONE = java.time.ZoneId.of("Asia/Seoul");

    private final WalkingPreferenceRepository walkingPreferenceRepository;
    private final WeatherRecommendationMessageComposer weatherRecommendationMessageComposer;

    @Override
    public List<PushTask> createPushTasks(List<EnabledNotifications> memberSettings) {
        if (memberSettings == null || memberSettings.isEmpty()) {
            return List.of();
        }

        // 추천 시간은 발송 시점의 평일/주말 여부로 결정한다.
        LocalDateTime referenceTime = LocalDateTime.now(KOREA_ZONE);

        // NotificationProcessor가 조회한 현재 청크의 회원만 한 번에 스냅샷으로 조회한다.
        List<UUID> memberIds = memberSettings.stream().map(EnabledNotifications::memberId).toList();

        List<WeatherRecommendationTarget> targets = findTargets(memberIds, referenceTime);

        log.info("event=snapshot_weather_recommendation_targets_resolved, recipientCount={}, targetCount={}",
                memberSettings.size(), targets.size());

        // 날씨 조회와 TokenPushTask 생성은 두 추천 정책이 공유하는 Composer에 위임한다.
        return weatherRecommendationMessageComposer.createPushTasks(memberSettings, targets, referenceTime);
    }

    private List<WeatherRecommendationTarget> findTargets(List<UUID> memberIds, LocalDateTime referenceTime) {
        List<UUID> distinctMemberIds = memberIds.stream().filter(Objects::nonNull).distinct().toList();

        // 7일 이내 스냅샷만 허용한다. 오래된 산책 패턴은 추천 근거로 사용하지 않는다.
        Map<UUID, WalkingPreference> latestByMemberId = walkingPreferenceRepository
                .findByMemberIdsAndCreatedAtBetween(
                        distinctMemberIds, referenceTime.minusDays(PREFERENCE_VALID_DAYS), referenceTime)
                .stream()
                .collect(Collectors.toMap(
                        preference -> preference.getMember().getId(), Function.identity(), this::latestOf));
        boolean weekend = isWeekend(referenceTime.getDayOfWeek());

        return distinctMemberIds.stream()
                // 스냅샷이 없거나 해당 요일의 선호 시간이 없으면 알림 대상에서 제외된다.
                .map(memberId -> toTarget(memberId, latestByMemberId.get(memberId), weekend))
                .flatMap(java.util.Optional::stream)
                .toList();
    }

    private WalkingPreference latestOf(WalkingPreference first, WalkingPreference second) {
        return Comparator.comparing(WalkingPreference::getCreatedAt).thenComparing(WalkingPreference::getId)
                .compare(first, second) >= 0 ? first : second;
    }

    private java.util.Optional<WeatherRecommendationTarget> toTarget(
            UUID memberId, WalkingPreference preference, boolean weekend
    ) {
        if (preference == null) {
            return java.util.Optional.empty();
        }
        LocalTime time = weekend ? preference.getWeekendTime() : preference.getWeekdayTime();
        return time == null ? java.util.Optional.empty() : java.util.Optional.of(new WeatherRecommendationTarget(
                memberId, preference.getLastKnownLatitude(), preference.getLastKnownLongitude(), time));
    }

    private boolean isWeekend(DayOfWeek dayOfWeek) {
        return dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY;
    }
}
