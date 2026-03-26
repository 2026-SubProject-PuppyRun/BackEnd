package org.zerock.puppyrun.statistics.controller.Response;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.Builder;
import org.zerock.puppyrun.statistics.DTO.WeeklyActivityChart;
import org.zerock.puppyrun.tracking.DTO.TotalPetTracking;

@Builder
public record WeeklyActivityResponse(
        Period period,
        Summary summary,
        List<ActivityChart> activityChart,
        FamilyReport familyReport
) {
    private static final double METERS_TO_KM = 1000.0;
    private static final int SECONDS_TO_MINUTES = 60;

    @Builder
    public record Period(
            String type,
            LocalDate startDate,
            LocalDate endDate
    ) {
        public static Period from(WeeklyActivityChart chart) {
            return Period.builder()
                    .type("weekly")
                    .startDate(chart.startDate())
                    .endDate(chart.endDate())
                    .build();
        }
    }

    @Builder
    public record Summary(
            Double totalDistanceKm,
            Integer totalDurationMin,
            Integer totalCount
    ) {
        public static Summary of(List<ActivityChart> activityCharts) {
            double totalDistanceKm = activityCharts.stream().mapToDouble(ActivityChart::distanceKm).sum();
            int totalDurationMin = activityCharts.stream().mapToInt(ActivityChart::durationMin).sum();
            int totalCount = activityCharts.stream().mapToInt(ac -> ac.durationMin() > 0 ? 1 : 0).sum();

            return Summary.builder()
                    .totalDistanceKm(Math.round(totalDistanceKm * 10) / 10.0)
                    .totalDurationMin(totalDurationMin)
                    .totalCount(totalCount)
                    .build();
        }
    }

    @Builder
    public record ActivityChart(
            LocalDate date,
            String label,
            Double distanceKm,
            Integer durationMin
    ) {
        public static List<ActivityChart> listOf(List<WeeklyActivityChart.ActivityChart> charts) {
            return charts.stream()
                    .map(ActivityChart::from)
                    .toList();
        }

        private static ActivityChart from(WeeklyActivityChart.ActivityChart ac) {
            return ActivityChart.builder()
                    .date(ac.date())
                    .label(ac.label())
                    .distanceKm(Math.round((ac.distance() / METERS_TO_KM) * 10) / 10.0) // 소수점 첫째 자리 반올림
                    .durationMin(ac.duration() / SECONDS_TO_MINUTES)
                    .build();
        }
    }

    @Builder
    public record FamilyReport(
            Integer totalDogs,
            List<DogStat> dogStats
    ) {
        public static FamilyReport of(List<TotalPetTracking> summaries) {
            double allDogsTotalDistance = summaries.stream()
                    .mapToDouble(TotalPetTracking::totalDistance)
                    .sum();

            List<DogStat> dogStats = summaries.stream()
                    .map(ps -> DogStat.of(ps, allDogsTotalDistance))
                    .toList();

            return FamilyReport.builder()
                    .totalDogs(summaries.size())
                    .dogStats(dogStats)
                    .build();
        }
    }

    @Builder
    public record DogStat(
            UUID dogId,
            String name,
            String profileImageUrl,
            String themeColor,
            Double distanceKm,
            Integer durationMin,
            Double sharePercentage,
            Integer totalCount,
            String badge
    ) {
        public static DogStat of(TotalPetTracking ps, double allDogsTotalDistance) {
            double distanceKm = ps.totalDistance() / METERS_TO_KM;
            // 전체 거리가 0 초과일 때만 비율 계산 (0으로 나누기 방지)
            double sharePercentage = allDogsTotalDistance > 0
                    ? (ps.totalDistance() / allDogsTotalDistance) * 100
                    : 0.0;

            return DogStat.builder()
                    .dogId(ps.petId())
                    .name(ps.name())
                    .profileImageUrl(ps.profileImageUrl())
                    .themeColor(ps.themeColor())
                    .distanceKm(Math.round(distanceKm * 10) / 10.0)
                    .durationMin(ps.totalDuration() / 60)
                    .sharePercentage(Math.round(sharePercentage * 10) / 10.0)
                    .totalCount(ps.totalCount().intValue())
                    .badge(ps.badge().getCode())
                    .build();
        }
    }

    /**
     * 통계 데이터를 조합하여 WeeklyActivityResponse로 변환하는 팩토리 메서드
     */
    public static WeeklyActivityResponse of(
            WeeklyActivityChart chart,
            List<TotalPetTracking> summaries
    ) {
        // 내부 레코드에게 위임하여 응집도를 높인 객체 조립
        List<ActivityChart> activityCharts = ActivityChart.listOf(chart.activityChart());

        return WeeklyActivityResponse.builder()
                .period(Period.from(chart))
                .summary(Summary.of(activityCharts))
                .activityChart(activityCharts)
                .familyReport(FamilyReport.of(summaries))
                .build();
    }
}
