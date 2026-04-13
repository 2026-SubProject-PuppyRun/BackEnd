package org.zerock.puppyrun.statistics.controller.Response;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.zerock.puppyrun.statistics.DTO.WeeklyActivityChart.ActivityChart;

import java.util.List;
import java.util.function.ToDoubleFunction;

@Getter
@RequiredArgsConstructor
public enum RadarMetric {
    DISTANCE("총 이동 거리 (km)", 30.0, RadarMetric::calculateDistance),
    SPEED("평균 이동 속도 (km/h)", 10.0, RadarMetric::calculateSpeed),
    FREQUENCY("산책 빈도 (일)", 7.0, RadarMetric::calculateFrequency),
    REST_TIME("휴식 시간 (분)", 120.0, RadarMetric::calculateRestTime);

    private final String label;
    private final Double maxScore;
    private final ToDoubleFunction<List<ActivityChart>> calculator;

    private static final double METERS_TO_KM = 1000.0;
    private static final double SECONDS_TO_MINUTES = 60.0;

    private static double calculateDistance(List<ActivityChart> charts) {
        double totalDist = charts.stream()
                .mapToDouble(c -> c.distance() == null ? 0.0 : c.distance())
                .sum() / METERS_TO_KM;
        return Math.round(totalDist * 10) / 10.0;
    }

    private static double calculateSpeed(List<ActivityChart> charts) {
        double totalDist = calculateDistance(charts); // 내부 재사용
        int totalDurationSec = charts.stream()
                .mapToInt(c -> c.duration() == null ? 0 : c.duration())
                .sum();
        if (totalDurationSec == 0) {
            return 0.0;
        }
        double speed = totalDist / (totalDurationSec / 3600.0);
        return Math.round(speed * 10) / 10.0;
    }

    private static double calculateFrequency(List<ActivityChart> charts) {
        return charts.stream()
                .filter(c -> c.duration() != null && c.duration() > 0)
                .count();
    }

    private static double calculateRestTime(List<ActivityChart> charts) {
        double totalMinutes = charts.stream()
                .mapToInt(c -> c.restDuration() == null ? 0 : c.restDuration())
                .sum() / SECONDS_TO_MINUTES;
        return Math.round(totalMinutes * 100.0) / 100.0;
    }
}
