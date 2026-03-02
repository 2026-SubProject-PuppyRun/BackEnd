package org.zerock.puppyrun.tracking.controller;

import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.zerock.puppyrun.common.auth.security.UserPrincipal;
import org.zerock.puppyrun.tracking.controller.request.RegisterTrackingRequest;
import org.zerock.puppyrun.tracking.controller.response.MainTrackingResponse;
import org.zerock.puppyrun.tracking.controller.response.TrackingDetailResponse;
import org.zerock.puppyrun.tracking.service.TrackingService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/tracking")
public class TrackingController {
    private final TrackingService trackingService;

    // 산책 저장
    @PostMapping("")
    public ResponseEntity<String> saveTracking(@Valid @RequestBody RegisterTrackingRequest request,
                                               @AuthenticationPrincipal UserPrincipal userPrincipal) {

        trackingService.saveTracking(userPrincipal.id(), request);

        return ResponseEntity.ok("산책 저장 완료");
    }

    // 산책 기록 조회
    @GetMapping("")
    public ResponseEntity<MainTrackingResponse> getTrackingList(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        MainTrackingResponse response = trackingService.getTrackingListResponse(userPrincipal.id());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{trackingId}")
    public ResponseEntity<TrackingDetailResponse> getTracking(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable UUID trackingId) {

        TrackingDetailResponse response = trackingService.getTrackingResponse(userPrincipal.id(), trackingId);

        return ResponseEntity.ok(response);
    }

}
