package org.zerock.puppyrun.statistics.DTO;

import java.time.LocalDate;
import java.util.List;
import lombok.Builder;
import org.zerock.puppyrun.tracking.DTO.DailyTrackingSummary;

public record WeeklyActivityChart(
        LocalDate startDate,
        LocalDate endDate,
        List<ActivityChart> activityChart
) {
    @Builder
    public record ActivityChart(
            LocalDate date,
            String label,
            Integer distance,
            Integer duration,
            Integer restDuration
    ) {
    }

    public static WeeklyActivityChart of(LocalDate startDate, LocalDate endDate,
                                         List<DailyTrackingSummary> summaryList) {
        List<ActivityChart> activityChartList = summaryList.stream()
                .map(summary -> ActivityChart.builder()
                        .date(summary.date())
                        .label(summary.date().getDayOfWeek().name())
                        .distance(summary.distance())
                        .duration(summary.duration())
                        .restDuration(summary.restDuration())
                        .build())
                .toList();

        return new WeeklyActivityChart(startDate, endDate, activityChartList);
    }
}
