package org.zerock.puppyrun.statistics.controller.Response;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.zerock.puppyrun.tracking.DTO.TotalPetTracking;

import java.util.function.ToDoubleFunction;

@Getter
@RequiredArgsConstructor
public enum RadarMetric {
    DISTANCE("총 이동 거리 (m)", 30_000.0, RadarMetric::calculateDistance),
    DURATION("총 산책 시간 (초)", 21_600.0, RadarMetric::calculateDuration),
    SPEED("평균 이동 속도 (km/h)", 10.0, RadarMetric::calculateSpeed),
    FREQUENCY("산책 빈도 (회)", 7.0, RadarMetric::calculateFrequency),
    REST_TIME("휴식 시간 (초)", 7_200.0, RadarMetric::calculateRestTime);

    private final String label;
    private final Double maxScore; // 차트 렌더링용 만점 기준

    private final ToDoubleFunction<TotalPetTracking> calculator;

    private static double calculateDistance(TotalPetTracking tracking) {
        if (tracking == null || tracking.totalDistance() == null) {
            return 0.0;
        }
        return tracking.totalDistance().doubleValue();
    }

    private static double calculateDuration(TotalPetTracking tracking) {
        if (tracking == null || tracking.totalDuration() == null) {
            return 0.0;
        }
        return tracking.totalDuration().doubleValue();
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
        return tracking.restDuration().doubleValue();
    }
}
