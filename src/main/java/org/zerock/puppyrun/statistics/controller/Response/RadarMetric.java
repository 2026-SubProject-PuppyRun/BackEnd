package org.zerock.puppyrun.statistics.controller.Response;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.zerock.puppyrun.tracking.DTO.TotalPetTracking;

import java.util.function.ToDoubleFunction;

@Getter
@RequiredArgsConstructor
public enum RadarMetric {
    DISTANCE("총 이동 거리 (km)", 30.0, RadarMetric::calculateDistance),
    DURATION("총 산책 시간 (분)", 360.0, RadarMetric::calculateDuration),
    SPEED("평균 이동 속도 (km/h)", 10.0, RadarMetric::calculateSpeed),
    FREQUENCY("산책 빈도 (회)", 7.0, RadarMetric::calculateFrequency),
    REST_TIME("휴식 시간 (분)", 120.0, RadarMetric::calculateRestTime);

    private final String label;
    private final Double maxScore; // 차트 렌더링용 만점 기준

    private final ToDoubleFunction<TotalPetTracking> calculator;

    private static final double METERS_TO_KM = 1000.0;
    private static final double SECONDS_TO_MINUTES = 60.0;

    private static double calculateDistance(TotalPetTracking tracking) {
        if (tracking == null || tracking.totalDistance() == null) {
            return 0.0;
        }
        double distKm = tracking.totalDistance() / METERS_TO_KM;
        return Math.round(distKm * 10) / 10.0;
    }

    private static double calculateDuration(TotalPetTracking tracking) {
        if (tracking == null || tracking.totalDuration() == null) {
            return 0.0;
        }
        double durationMin = tracking.totalDuration() / SECONDS_TO_MINUTES;
        return Math.round(durationMin * 10.0) / 10.0;
    }

    private static double calculateSpeed(TotalPetTracking tracking) {
        if (tracking == null || tracking.averageSpeed() == null) {
            return 0.0;
        }
        return Math.round(tracking.averageSpeed() * 10) / 10.0;
    }

    private static double calculateFrequency(TotalPetTracking tracking) {
        if (tracking == null || tracking.totalCount() == null) {
            return 0.0;
        }
        return tracking.totalCount().doubleValue();
    }

    private static double calculateRestTime(TotalPetTracking tracking) {
        if (tracking == null || tracking.restDuration() == null) {
            return 0.0;
        }
        double restMin = tracking.restDuration() / SECONDS_TO_MINUTES;
        return Math.round(restMin * 100.0) / 100.0;
    }
}
