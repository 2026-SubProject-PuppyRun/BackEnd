package org.zerock.puppyrun.tracking.recommendation.controller;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.zerock.puppyrun.tracking.recommendation.controller.response.RecommendedRouteResponse;
import org.zerock.puppyrun.tracking.recommendation.service.TrackingRecommendationService;

/**
 * 사용자 위치 기반 산책 경로 추천 API를 제공합니다.
 */
@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/api/tracking/recommendations")
public class TrackingRecommendationController {
    private final TrackingRecommendationService trackingRecommendationService;

    /**
     * 사용자 현재 위치 주변에서 시작하는 공개 산책 경로를 조회합니다.
     *
     * @param latitude 사용자 현재 위도(-90~90)
     * @param longitude 사용자 현재 경도(-180~180)
     * @param radiusMeters 검색 반경(m), 기본 3km
     * @param limit 최대 추천 개수, 기본 10개
     * @return 사용자 위치와 가까운 순서로 정렬된 추천 경로
     */
    @GetMapping
    public ResponseEntity<RecommendedRouteResponse> getRecommendedRoutes(
            @RequestParam
            @DecimalMin(value = "-90.0", message = "위도는 -90 이상이어야 합니다.")
            @DecimalMax(value = "90.0", message = "위도는 90 이하여야 합니다.")
            double latitude,
            @RequestParam
            @DecimalMin(value = "-180.0", message = "경도는 -180 이상이어야 합니다.")
            @DecimalMax(value = "180.0", message = "경도는 180 이하여야 합니다.")
            double longitude,
            @RequestParam(defaultValue = "3000")
            @Min(value = 100, message = "검색 반경은 100m 이상이어야 합니다.")
            @Max(value = 50000, message = "검색 반경은 50km 이하여야 합니다.")
            int radiusMeters,
            @RequestParam(defaultValue = "10")
            @Min(value = 1, message = "추천 개수는 1개 이상이어야 합니다.")
            @Max(value = 50, message = "추천 개수는 50개 이하여야 합니다.")
            int limit) {
        RecommendedRouteResponse response = trackingRecommendationService.getRecommendedRoutes(
                latitude,
                longitude,
                radiusMeters,
                limit
        );
        return ResponseEntity.ok(response);
    }
}
