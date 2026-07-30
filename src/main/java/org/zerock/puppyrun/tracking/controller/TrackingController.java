package org.zerock.puppyrun.tracking.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.validation.annotation.Validated;
import org.zerock.puppyrun.common.auth.security.UserPrincipal;
import org.zerock.puppyrun.tracking.controller.request.RegisterTrackingRequest;
import org.zerock.puppyrun.tracking.controller.response.MainTrackingResponse;
import org.zerock.puppyrun.tracking.controller.response.RecommendedRouteResponse;
import org.zerock.puppyrun.tracking.controller.response.TrackingDetailResponse;
import org.zerock.puppyrun.tracking.controller.request.ChangeVisibilityRequest;
import org.zerock.puppyrun.tracking.controller.request.UpdateTrackingRequest;
import org.zerock.puppyrun.tracking.service.TrackingCommandService;
import org.zerock.puppyrun.tracking.service.TrackingQueryService;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/api/tracking")
public class TrackingController {
    private final TrackingCommandService trackingCommandService;
    private final TrackingQueryService trackingQueryService;


    // 산책 저장
    @PostMapping("")
    public ResponseEntity<String> saveTracking(
            @Valid @RequestPart("request") RegisterTrackingRequest request,
            @RequestPart("images") List<MultipartFile> images,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        trackingCommandService.saveTracking(userPrincipal.id(), request, images);

        return ResponseEntity.ok("산책 저장 완료");
    }

    // 산책 기록 조회
    @GetMapping("")
    public ResponseEntity<MainTrackingResponse> getTrackingList(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        MainTrackingResponse response = trackingQueryService.getTrackingListResponse(userPrincipal.id());
        return ResponseEntity.ok(response);
    }

    /**
     * 사용자 현재 위치 주변에서 시작하는 공개 산책 경로를 조회합니다.
     *
     * @param latitude 사용자 현재 위도(-90~90)
     * @param longitude 사용자 현재 경도(-180~180)
     * @param radiusMeters 검색 반경(m), 기본 3km
     * @param limit 최대 추천 개수, 기본 10개
     * @return 사용자 위치와 가까운 순서로 정렬된 추천 경로
     */
    @GetMapping("/recommendations")
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
        RecommendedRouteResponse response = trackingQueryService.getRecommendedRoutes(
                latitude,
                longitude,
                radiusMeters,
                limit
        );
        return ResponseEntity.ok(response);
    }

    // 산책 기록 상세 조회
    @GetMapping("/{trackingId}")
    public ResponseEntity<TrackingDetailResponse> getTracking(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable UUID trackingId) {

        TrackingDetailResponse response = trackingQueryService.getTrackingResponse(userPrincipal.id(), trackingId);

        return ResponseEntity.ok(response);
    }

    // 산책 정보 수정 (전체 수정)
    @PutMapping("/{trackingId}")
    public ResponseEntity<TrackingDetailResponse> updateTracking(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable UUID trackingId,
            @RequestBody UpdateTrackingRequest request) {

        TrackingDetailResponse response = trackingCommandService.updateTracking(userPrincipal.id(), trackingId,
                request);

        return ResponseEntity.ok(response);
    }

    // 산책 공개 여부 변경 (부분 수정)
    @PatchMapping("/{trackingId}/visibility")
    public ResponseEntity<Void> changeVisibility(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable UUID trackingId,
            @RequestBody ChangeVisibilityRequest request) { // {"visibility": "PUBLIC"} 형태의 JSON 요청 처리

        trackingCommandService.changeTrackingVisibility(userPrincipal.id(), trackingId, request);

        return ResponseEntity.ok().build();
    }

    // 산책 정보 삭제
    @DeleteMapping("/{trackingId}")
    public ResponseEntity<Void> deleteTracking(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable UUID trackingId) {

        trackingCommandService.deleteTracking(userPrincipal.id(), trackingId);

        return ResponseEntity.ok().build();
    }
}
