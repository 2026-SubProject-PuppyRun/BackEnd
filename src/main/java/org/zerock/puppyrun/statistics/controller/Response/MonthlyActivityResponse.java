package org.zerock.puppyrun.statistics.controller.Response;

import java.time.LocalDate;
import java.util.List;
import lombok.Builder;
import org.zerock.puppyrun.statistics.DTO.MonthlyActivity;
import org.zerock.puppyrun.tracking.DTO.DailyTrackingSummary;


@Builder
public record MonthlyActivityResponse(
        Period period,
        List<MonthlySummary> monthlySummary, // 월 전체 산책 요약
        List<ContributionChart> contributionChart // 지난 15주의 산책 요약
) {

    @Builder
    public record Period(
            String type,
            String year
    ) {
        public static Period from(String year) {
            return Period.builder()
                    .type("monthly")
                    .year(year)
                    .build();
        }
    }

    @Builder
    public record MonthlySummary(
            String label, // 월 표시
            Integer totalDistanceM,
            Integer totalDurationSec,
            Integer totalCount
    ) {
        private static MonthlySummary from(MonthlyActivity activity) {
            return MonthlySummary.builder()
                    .label(activity.month().name())
                    .totalDistanceM(activity.totalDistance())
                    .totalDurationSec(activity.totalDuration())
                    .totalCount(activity.trackingCount())
                    .build();
        }

        private static List<MonthlySummary> listOf(List<MonthlyActivity> activity) {
            return activity.stream()
                    .map(MonthlySummary::from)
                    .toList();
        }
    }


    @Builder
    public record ContributionChart(
            LocalDate label,
            Integer distanceM,
            Integer durationSec,
            Integer trackingCount
    ) {
        private static ContributionChart from(DailyTrackingSummary ac) {
            return ContributionChart.builder()
                    .label(ac.date())
                    .distanceM(ac.distance())
                    .durationSec(ac.duration())
                    .trackingCount(ac.trackingCount())
                    .build();
        }

        public static List<ContributionChart> listOf(List<DailyTrackingSummary> charts) {
            return charts.stream()
                    .map(ContributionChart::from)
                    .toList();
        }
    }

    /**
     * 통계 데이터를 조합하여 변환하는 팩토리 메서드
     */
    public static MonthlyActivityResponse of(LocalDate targetDate, List<MonthlyActivity> activity,
                                             List<DailyTrackingSummary> fifteenContribution) {
        String year = String.valueOf(targetDate.getYear());
        return MonthlyActivityResponse.builder()
                .period(Period.from(year))
                .monthlySummary(MonthlySummary.listOf(activity))
                .contributionChart(ContributionChart.listOf(fifteenContribution))
                .build();
    }
}
