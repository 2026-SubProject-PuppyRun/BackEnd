package org.zerock.puppyrun.tracking.controller;

import jakarta.validation.Valid;
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
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.zerock.puppyrun.common.auth.security.UserPrincipal;
import org.zerock.puppyrun.tracking.controller.request.RegisterTrackingRequest;
import org.zerock.puppyrun.tracking.controller.response.MainTrackingResponse;
import org.zerock.puppyrun.tracking.controller.response.TrackingDetailResponse;
import org.zerock.puppyrun.tracking.controller.request.ChangeVisibilityRequest;
import org.zerock.puppyrun.tracking.controller.request.UpdateTrackingRequest;
import org.zerock.puppyrun.tracking.service.TrackingCommandService;
import org.zerock.puppyrun.tracking.service.TrackingQueryService;

@RestController
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
