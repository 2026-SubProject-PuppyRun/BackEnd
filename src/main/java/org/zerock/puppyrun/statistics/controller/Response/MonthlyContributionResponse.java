package org.zerock.puppyrun.statistics.controller.Response;

import java.time.LocalDate;
import java.time.Month;
import java.util.List;
import lombok.Builder;
import org.zerock.puppyrun.statistics.DTO.MonthlyActivity;

@Builder
public record MonthlyContributionResponse(
        Period period,
        List<ActivityChart> activityChart // 해당 월의 일일 산책 요약

) {

    private static final double METERS_TO_KM = 1000.0;
    private static final int SECONDS_TO_MINUTES = 60;

    @Builder
    public record Period(
            String type,
            String month
    ) {
        public static Period from(Month month) {
            return Period.builder()
                    .type("contributions")
                    .month(month.name())
                    .build();
        }
    }


    @Builder
    public record ActivityChart(
            LocalDate label,
            Double distanceKm,
            Integer durationMin,
            Integer trackingCount
    ) {
        private static ActivityChart from(MonthlyActivity.ActivityChart ac) {
            return ActivityChart.builder()
                    .label(ac.date())
                    .distanceKm(Math.round(ac.totalDistance() / METERS_TO_KM * 10) / 10.0)
                    .durationMin(ac.totalDuration() / SECONDS_TO_MINUTES)
                    .trackingCount(ac.trackingCount())
                    .build();
        }

        public static List<ActivityChart> listOf(List<MonthlyActivity.ActivityChart> charts) {
            return charts.stream()
                    .map(ActivityChart::from)
                    .toList();
        }
    }

    public static MonthlyContributionResponse of(LocalDate targetDate, MonthlyActivity activity) {
        return MonthlyContributionResponse.builder()
                .period(Period.from(targetDate.getMonth()))
                .activityChart(ActivityChart.listOf(activity.activityChart()))
                .build();
    }
}
