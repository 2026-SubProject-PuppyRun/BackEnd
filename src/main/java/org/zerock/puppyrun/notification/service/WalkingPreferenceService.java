package org.zerock.puppyrun.notification.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.zerock.puppyrun.common.logging.LogExecutionTime;
import org.zerock.puppyrun.notification.entity.WalkingPreference;
import org.zerock.puppyrun.notification.repository.WalkingPreferenceRepository;
import org.zerock.puppyrun.tracking.entity.Tracking;
import org.zerock.puppyrun.tracking.repository.TrackingRepository;

/**
 * 최근 산책 기록을 분석해 회원별 선호 산책 시간대를 갱신합니다.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class WalkingPreferenceService {

    private static final int ANALYSIS_WINDOW_DAYS = 30;
    private static final int BUCKET_SIZE_HOURS = 2;
    private static final int CHUNK_SIZE = 300;

    private final TrackingRepository trackingRepository;
    private final WalkingPreferenceRepository walkingPreferenceRepository;

    /**
     * 최근 30일 내 산책 회원을 청크 단위로 조회해 평일·주말 선호 시간을 갱신합니다.
     */
    @LogExecutionTime
    public void updateAllMemberPreferences() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startDateTime = now.minusDays(ANALYSIS_WINDOW_DAYS);
        int pageNumber = 0;
        List<UUID> memberIds;

        do {
            Pageable pageable = PageRequest.of(pageNumber, CHUNK_SIZE);
            memberIds = trackingRepository.findActiveMemberIds(startDateTime, pageable);
            if (memberIds.isEmpty()) {
                break;
            }
            List<Tracking> trackings = trackingRepository
                    .findAllByMemberIdsAndDateRange(memberIds, startDateTime);
            Map<UUID, List<Tracking>> trackingMap = trackings.stream()
                    .filter(tracking -> tracking.getMember().getId() != null)
                    .collect(Collectors.groupingBy(tracking -> tracking.getMember().getId()));
            Map<UUID, WalkingPreference> preferenceMap = walkingPreferenceRepository.findAllById(memberIds)
                    .stream()
                    .collect(Collectors.toMap(preference -> preference.getMember().getId(), preference -> preference));

            List<WalkingPreference> updates = new ArrayList<>();
            for (UUID memberId : memberIds) {
                WalkingPreference preference = preferenceMap.get(memberId);
                List<Tracking> memberTrackings = trackingMap.getOrDefault(memberId, List.of());
                if (preference == null || memberTrackings.isEmpty()) {
                    continue;
                }

                updatePreference(preference, memberTrackings, now.toLocalDate());
                updates.add(preference);
            }

            walkingPreferenceRepository.saveAll(updates);
            pageNumber++;
        } while (memberIds.size() == CHUNK_SIZE);
    }

    private void updatePreference(
            WalkingPreference preference,
            List<Tracking> trackings,
            LocalDate today
    ) {
        Map<Integer, Double> weekdayScores = new HashMap<>();
        Map<Integer, Double> weekendScores = new HashMap<>();

        for (Tracking tracking : trackings) {
            LocalDateTime startedAt = tracking.getStartedAt();
            int bucketIndex = startedAt.getHour() / BUCKET_SIZE_HOURS;
            long daysAgo = ChronoUnit.DAYS.between(startedAt.toLocalDate(), today);
            double weight = calculateTimeDecayWeight(daysAgo);

            if (isWeekend(startedAt.getDayOfWeek())) {
                weekendScores.merge(bucketIndex, weight, Double::sum);
            } else {
                weekdayScores.merge(bucketIndex, weight, Double::sum);
            }
        }

        preference.updateTimePreferences(getBestHour(weekdayScores), getBestHour(weekendScores));
    }

    private boolean isWeekend(DayOfWeek dayOfWeek) {
        return dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY;
    }

    private double calculateTimeDecayWeight(long daysAgo) {
        double halfLife = 10.0;
        double lambda = Math.log(2) / halfLife;
        return Math.max(0.1, Math.exp(-lambda * daysAgo));
    }

    private Integer getBestHour(Map<Integer, Double> scores) {
        return scores.entrySet().stream()
                .max(Comparator.comparingDouble(Map.Entry::getValue))
                .map(entry -> entry.getKey() * BUCKET_SIZE_HOURS)
                .orElse(null);
    }
}
