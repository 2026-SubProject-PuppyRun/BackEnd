package org.zerock.puppyrun.tracking.service;

import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.zerock.puppyrun.common.exception.ResourceNotFoundException;
import org.zerock.puppyrun.common.exception.UserForbiddenException;
import org.zerock.puppyrun.member.entity.Member;
import org.zerock.puppyrun.member.repository.MemberRepository;
import org.zerock.puppyrun.tracking.controller.request.ResistedTrackingRequest;
import org.zerock.puppyrun.tracking.controller.response.MainTrackingResponse;
import org.zerock.puppyrun.tracking.controller.response.TrackingDetailResponse;
import org.zerock.puppyrun.tracking.entity.Tracking;
import org.zerock.puppyrun.tracking.entity.TrackingPath;
import org.zerock.puppyrun.tracking.entity.Visibility;
import org.zerock.puppyrun.tracking.repository.TrackingRepository;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class TrackingService {
    private final TrackingRepository trackingRepository;
    private final MemberRepository memberRepository;

    /**
     * 산책 상세 조회
     */
    public Tracking getTracking(UUID memberId, UUID trackingId) {
        Tracking tracking = trackingRepository.findById(trackingId)
                .orElseThrow(() -> new ResourceNotFoundException("해당 산책 기록을 찾을 수 없습니다."));

        if (!tracking.isOwner(memberId)) {
            throw new UserForbiddenException("해당 산책 기록에 대한 권한이 없습니다.");
        }
        return tracking;
    }

    /**
     * 산책 저장
     */
    @Transactional
    public void saveTracking(UUID memberId, ResistedTrackingRequest request) {
        Member member = memberRepository.findByIdOrThrow(memberId);

        List<TrackingPath> path = request.path().stream()
                .map(point -> new TrackingPath(point.lat(), point.lng(), point.time()))
                .toList();

        TrackingPath startPoint = path.getFirst();

        Tracking tracking = Tracking.builder()
                .member(member)
                .startedAt(request.startedAt())
                .endedAt(request.endedAt())
                .startedLat(startPoint.getLat())
                .startedLng(startPoint.getLng())
                .visibility(Visibility.from(request.visibility()))
                .distance(request.distance())
                .path(path)
                .build();
    }

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
        Tracking tracking = getTracking(memberId, trackingId);
        return TrackingDetailResponse.from(tracking);
    }


}
