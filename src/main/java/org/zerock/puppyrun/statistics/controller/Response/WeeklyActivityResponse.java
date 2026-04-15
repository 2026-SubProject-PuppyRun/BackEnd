package org.zerock.puppyrun.statistics.controller.Response;

import java.time.LocalDate;
import java.util.Arrays;
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
        FamilyReport familyReport,
        List<DogRadar> dogRadars // 강아지별 방사형 데이터 리스트로 변경
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

    // 개별 강아지의 방사형 차트 데이터를 담는 객체
    @Builder
    public record DogRadar(
            UUID dogId,
            String dogName,
            String profileImageUrl,
            String themeColor,
            List<RadarDataPoint> dataPoints
    ) {
        @Builder
        public record RadarDataPoint(
                String metricCode,    // "DISTANCE"
                String label,         // "총 이동 거리 (km)"
                Double thisWeekValue, // 이번 주 값
                Double lastWeekValue, // 저번 주 값
                Double maxScore       // 만점 기준 (차트 렌더링용)
        ) {
        }

        public static List<DogRadar> of(List<TotalPetTracking> thisWeekSummaries,
                                        List<TotalPetTracking> lastWeekSummaries) {
            return thisWeekSummaries.stream().map(thisWeek -> {
                TotalPetTracking lastWeek = lastWeekSummaries.stream()
                        .filter(lw -> lw.petId().equals(thisWeek.petId()))
                        .findFirst()
                        .orElse(null);

                List<RadarDataPoint> points = Arrays.stream(RadarMetric.values())
                        .map(metric -> RadarDataPoint.builder()
                                .metricCode(metric.name())
                                .label(metric.getLabel())
                                .thisWeekValue(metric.getCalculator().applyAsDouble(thisWeek))
                                .lastWeekValue(metric.getCalculator().applyAsDouble(lastWeek))
                                .maxScore(metric.getMaxScore())
                                .build()
                        ).toList();

                return DogRadar.builder()
                        .dogId(thisWeek.petId())
                        .dogName(thisWeek.name()) // TotalPetTracking의 강아지 이름 필드
                        .themeColor(thisWeek.themeColor())
                        .profileImageUrl(thisWeek.profileImageUrl())
                        .dataPoints(points)
                        .build();
            }).toList();

        }
    }

    @Builder
    public record ActivityChart(
            LocalDate date,
            String label,
            Double distanceKm,
            Integer durationMin
    ) {
        public static List<ActivityChart> listOf(List<WeeklyActivityChart.ActivityChart> charts, LocalDate targetDate) {
            LocalDate thisWeekStart = targetDate.minusDays(6);
            return charts.stream()
                    .filter(c -> !c.date().isBefore(thisWeekStart) && !c.date().isAfter(targetDate))
                    .map(ActivityChart::from)
                    .toList();
        }

        private static ActivityChart from(WeeklyActivityChart.ActivityChart ac) {
            return ActivityChart.builder()
                    .date(ac.date())
                    .label(ac.label())
                    .distanceKm(Math.round(((ac.distance() == null ? 0 : ac.distance()) / METERS_TO_KM) * 10) / 10.0)
                    .durationMin((ac.duration() == null ? 0 : ac.duration()) / SECONDS_TO_MINUTES)
                    .build();
        }
    }

    @Builder
    public record FamilyReport(
            Integer totalDogs,
            List<DogStat> dogStats
    ) {
        public static FamilyReport of(List<TotalPetTracking> thisWeekSummaries) {
            double allDogsTotalDistance = thisWeekSummaries.stream()
                    .mapToDouble(TotalPetTracking::totalDistance)
                    .sum();

            List<DogStat> dogStats = thisWeekSummaries.stream()
                    .map(ps -> DogStat.of(ps, allDogsTotalDistance))
                    .toList();

            return FamilyReport.builder()
                    .totalDogs(thisWeekSummaries.size())
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
                    .badge(ps.badge() != null ? ps.badge().getCode() : null)
                    .build();
        }
    }

    public static WeeklyActivityResponse of(
            WeeklyActivityChart chart,
            List<TotalPetTracking> thisWeekSummaries,
            List<TotalPetTracking> lastWeekSummaries,
            LocalDate targetDate
    ) {
        List<ActivityChart> activityCharts = ActivityChart.listOf(chart.activityChart(), targetDate);

        return WeeklyActivityResponse.builder()
                .period(Period.from(chart))
                .summary(Summary.of(activityCharts))
                .dogRadars(DogRadar.of(thisWeekSummaries, lastWeekSummaries))
                .activityChart(activityCharts)
                .familyReport(FamilyReport.of(thisWeekSummaries))
                .build();
    }
}
