package org.zerock.puppyrun.statistics.DTO;

import java.time.LocalDate;
import java.time.Month;
import java.util.List;
import lombok.Builder;
import org.zerock.puppyrun.tracking.DTO.DailyTrackingSummary;

@Builder
public record MonthlyActivity(
        Month month,
        Integer trackingCount,
        Integer totalDistance,
        Integer totalDuration,
        List<ActivityChart> activityChart
) {
    @Builder
    public record ActivityChart(
            LocalDate date,
            Integer trackingCount,
            Integer totalDistance,
            Integer totalDuration
    ) {
        public static ActivityChart from(DailyTrackingSummary summary) {
            return ActivityChart.builder()
                    .date(summary.date())
                    .trackingCount(summary.trackingCount())
                    .totalDistance(summary.distance())
                    .totalDuration(summary.duration())
                    .build();
        }

    }

    public static List<ActivityChart> listOf(List<DailyTrackingSummary> dailyTrackingSummaries) {
        return dailyTrackingSummaries.stream()
                .map(ActivityChart::from)
                .toList();

    }

    public static MonthlyActivity of(Month month, List<ActivityChart> activityChart) {

        int totalDuration = 0;
        int totalDistance = 0;
        int totalTrackingCount = 0;

        for (ActivityChart chart : activityChart) {
            totalDuration += chart.totalDuration();
            totalDistance += chart.totalDistance();
            totalTrackingCount += chart.trackingCount();
        }

        return MonthlyActivity.builder()
                .month(month)
                .totalDuration(totalDuration)
                .totalDistance(totalDistance)
                .trackingCount(totalTrackingCount)
                .activityChart(activityChart)
                .build();
    }
}
