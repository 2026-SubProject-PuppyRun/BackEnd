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
            Integer distanceM,
            Integer durationSec,
            Integer trackingCount
    ) {
        private static ActivityChart from(MonthlyActivity.ActivityChart ac) {
            return ActivityChart.builder()
                    .label(ac.date())
                    .distanceM(ac.totalDistance())
                    .durationSec(ac.totalDuration())
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
