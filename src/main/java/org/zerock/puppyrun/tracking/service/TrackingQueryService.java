package org.zerock.puppyrun.tracking.service;

import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.zerock.puppyrun.diary.repository.DiaryRepository;
import org.zerock.puppyrun.tracking.controller.response.MainTrackingResponse;
import org.zerock.puppyrun.tracking.controller.response.TrackingDetailResponse;
import org.zerock.puppyrun.tracking.entity.Tracking;
import org.zerock.puppyrun.tracking.entity.RoutePoint;
import org.zerock.puppyrun.tracking.entity.TrackingRoute;
import org.zerock.puppyrun.tracking.repository.TrackingRouteRepository;
import org.zerock.puppyrun.tracking.repository.TrackingRepository;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TrackingQueryService {
    private final TrackingRepository trackingRepository;
    private final TrackingRouteRepository trackingRouteRepository;
    private final DiaryRepository diaryRepository;

    /**
     * 산책 리스트 조회
     */
    public MainTrackingResponse getTrackingListResponse(UUID memberId) {
        List<Tracking> trackingList = trackingRepository.findAllByMemberId(memberId);
        return MainTrackingResponse.from(trackingList);
    }

    /**
     * 산책 상세 조회
     */
    public TrackingDetailResponse getTrackingResponse(UUID memberId, UUID trackingId) {
        Tracking tracking = trackingRepository.findByIdAndVerifyOwnership(trackingId, memberId);

        UUID diaryId = diaryRepository.findIdByTrackingId(trackingId).orElse(null);

        // 경로 데이터를 조회하여
        List<RoutePoint> path = trackingRouteRepository.findByTrackingId(trackingId)
                .map(TrackingRoute::getOriginalPath)
                .orElse(List.of());

        return TrackingDetailResponse.of(tracking, path, diaryId);
    }
}
