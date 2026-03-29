package org.zerock.puppyrun.statistics.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.temporal.TemporalAdjusters;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.zerock.puppyrun.pet.entity.Pet;
import org.zerock.puppyrun.statistics.DTO.DailyPetTracking;
import org.zerock.puppyrun.statistics.DTO.MonthlyActivity;
import org.zerock.puppyrun.tracking.DTO.DailyTracking;
import org.zerock.puppyrun.statistics.DTO.WeeklyActivityChart;
import org.zerock.puppyrun.statistics.DTO.WeeklyActivityChart.ActivityChart;
import org.zerock.puppyrun.tracking.DTO.DailyTrackingSummary;
import org.zerock.puppyrun.tracking.repository.PetTrackingRepository;
import org.zerock.puppyrun.tracking.repository.TrackingRepository;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class TrackingStatistics {
    private final TrackingRepository trackingRepository;
    private final PetTrackingRepository petTrackingRepository;

    /**
     * 주간 산책 차트 데이터를 조회합니다. (최근 7일)
     *
     * @param memberId   member Id
     * @param targetDate 요청할 date
     * @return WeeklyActivityChart
     */
    public WeeklyActivityChart getWeeklyChart(UUID memberId, LocalDate targetDate) {
        LocalDate startDate = targetDate.minusDays(6);

        List<DailyTrackingSummary> summaryList = trackingRepository.getTrackingSummaryDateAsc(memberId, startDate,
                targetDate);

        List<ActivityChart> activityChartList = summaryList.stream()
                .map(summary -> ActivityChart.builder()
                        .date(summary.date())
                        .label(summary.date().getDayOfWeek().name())
                        .distance(summary.distance())
                        .duration(summary.duration())
                        .build())
                .toList();

        return new WeeklyActivityChart(startDate, targetDate, activityChartList);
    }

    /**
     * 특정 날짜의 하루 상세 산책 내역을 반환합니다.
     */
    public List<DailyPetTracking> getDayActivity(UUID memberId, LocalDate targetDate) {
        List<DailyTracking> dailyTrackingList = trackingRepository.getDailyActivities(memberId, targetDate);

        // 산책 기록이 없으면 추가 로직 없이 즉시 종료
        if (dailyTrackingList.isEmpty()) {
            return Collections.emptyList();
        }

        return dailyTrackingList.stream()
                .map(dailyTracking -> {
                    List<Pet> petList = petTrackingRepository.findAllPetsByTrackingId(dailyTracking.trackingId());
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

    /**
     * 올해 1월 1일부터 조회 시점까지의 월별 산책 누적 통계를 조회합니다.
     */
    public List<MonthlyActivity> getMonthlyRecord(UUID memberId, LocalDate targetDate) {
        LocalDate startDate = targetDate.withDayOfYear(1);
        LocalDate endDate = targetDate.with(TemporalAdjusters.lastDayOfMonth());

        List<DailyTrackingSummary> trackingSummaryList =
                trackingRepository.getTrackingSummaryDateAsc(memberId, startDate, endDate);

        if (trackingSummaryList.isEmpty()) {
            return Collections.emptyList();
        }

        // 데이터를 월 단위(Month Enum)로 오름차순 그룹화
        Map<Month, List<DailyTrackingSummary>> groupedByMonth = trackingSummaryList.stream()
                .collect(Collectors.groupingBy(
                        dts -> dts.date().getMonth(),
                        TreeMap::new,
                        Collectors.toList()
                ));

        return groupedByMonth.entrySet().stream()
                .map(entry -> MonthlyActivity.of(
                        entry.getKey(),
                        MonthlyActivity.listOf(entry.getValue())
                ))
                .toList();
    }

    /**
     * 특정 달(1개월)의 잔디심기(기여도) 데이터를 조회합니다.
     */
    public MonthlyActivity getMonthlyContribution(UUID memberId, LocalDate targetDate) {
        LocalDate startDate = targetDate.withDayOfMonth(1);
        LocalDate endDate = targetDate.with(TemporalAdjusters.lastDayOfMonth());

        List<DailyTrackingSummary> trackingSummaryList =
                trackingRepository.getTrackingSummaryDateAsc(memberId, startDate, endDate);

        // 500 에러 방지: 산책 기록이 아예 없는 달인 경우, 비어있는 차트를 안전하게 반환
        if (trackingSummaryList.isEmpty()) {
            return MonthlyActivity.of(targetDate.getMonth(), Collections.emptyList());
        }

        List<MonthlyActivity.ActivityChart> activityChartList = MonthlyActivity.listOf(trackingSummaryList);

        return MonthlyActivity.of(targetDate.getMonth(), activityChartList);
    }
}
