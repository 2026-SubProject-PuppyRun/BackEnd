package org.zerock.puppyrun.statistics.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.zerock.puppyrun.pet.entity.Pet;
import org.zerock.puppyrun.statistics.DTO.DailyPetTracking;
import org.zerock.puppyrun.tracking.DTO.DailyTracking;
import org.zerock.puppyrun.statistics.DTO.WeeklyActivityChart;
import org.zerock.puppyrun.statistics.DTO.WeeklyActivityChart.ActivityChart;
import org.zerock.puppyrun.tracking.DTO.DailyTrackingSummary;
import org.zerock.puppyrun.tracking.repository.PetTrackingRepository;
import org.zerock.puppyrun.tracking.repository.TrackingRepository;

@Component
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class TrackingStatistics {
    private final TrackingRepository trackingRepository;
    private final PetTrackingRepository petTrackingRepository;


    /**
     * @param memberId   member Id
     * @param targetDate 요청할 date
     * @return WeeklyActivityChart
     */
    public WeeklyActivityChart getWeeklyChart(UUID memberId, LocalDate targetDate) {
        LocalDate startDate = targetDate.minusDays(6);

        List<DailyTrackingSummary> summaryList = trackingRepository.getDayTracking(memberId,
                startDate, targetDate);

        List<WeeklyActivityChart.ActivityChart> activityChartList = summaryList.stream()
                .map(summary -> {
                    String label = summary.date().getDayOfWeek().name();
                    return ActivityChart.builder()
                            .date(summary.date())
                            .label(label)
                            .distance(summary.distance())
                            .duration(summary.duration())
                            .build();
                })
                .toList();
        return new WeeklyActivityChart(startDate, targetDate, activityChartList);
    }

    /**
     * 특정 날짜의 하루 상세 산책 내역(요약 및 개별 리스트)을 반환합니다.
     */
    public List<DailyPetTracking> getDayActivity(UUID memberId, LocalDate targetDate) {
        List<DailyTracking> dailyTrackingList = trackingRepository.getDailyActivities(memberId, targetDate);

        // 산책 기록이 없으면 추가 로직 없이 즉시 종료
        if (dailyTrackingList.isEmpty()) {
            return java.util.Collections.emptyList();
        }

        return dailyTrackingList.stream()
                .map(dailyTracking -> {
                    UUID trackingId = dailyTracking.trackingId();
                    List<Pet> petList = petTrackingRepository.findAllPetsByTrackingId(trackingId);
                    return DailyPetTracking.of(dailyTracking, petList);
                })
                .toList();
    }

    /**
     * 소유한 펫이 일주일동안 같이 산책한 수를 조회합니다.
     *
     * @param memberId  멤버 아이디
     * @param targetDay 조회할 날짜
     * @return 같이 산책한 횟수
     */

    public int getTogetherWeeklyTrackingCount(UUID memberId, LocalDateTime targetDay) {
        LocalDate endDate = targetDay.toLocalDate();
        LocalDate startDate = endDate.minusDays(6);

        return petTrackingRepository.countTogetherTracking(memberId, startDate, endDate);
    }
}
