package org.zerock.puppyrun.tracking.service;

import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.zerock.puppyrun.common.exception.ResourceNotFoundException;
import org.zerock.puppyrun.common.exception.UserForbiddenException;
import org.zerock.puppyrun.tracking.DTO.MainTrackingSummary;
import org.zerock.puppyrun.tracking.DTO.TrackingDetailSummary;
import org.zerock.puppyrun.tracking.controller.response.MainTrackingResponse;
import org.zerock.puppyrun.tracking.controller.response.TrackingDetailResponse;
import org.zerock.puppyrun.tracking.repository.TrackingRepository;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TrackingQueryService {
    private final TrackingRepository trackingRepository;

    /**
     * 산책 리스트 조회
     */
    public MainTrackingResponse getTrackingListResponse(UUID memberId) {
        List<MainTrackingSummary> summaries = trackingRepository.findMainTrackingSummaries(memberId);
        return MainTrackingResponse.from(summaries);
    }

    /**
     * 산책 상세 조회
     */
    public TrackingDetailResponse getTrackingResponse(UUID memberId, UUID trackingId) {
        TrackingDetailSummary summary = trackingRepository.findTrackingDetailSummary(trackingId)
                .orElseThrow(() -> new ResourceNotFoundException("해당 산책 기록을 찾을 수 없습니다."));

        if (!summary.memberId().equals(memberId)) {
            throw new UserForbiddenException("해당 산책 기록에 대한 권한이 없습니다.");
        }

        // 이미지 목록 조회
        List<TrackingDetailSummary.TrackingImageSummary> images =
                trackingRepository.findTrackingImageSummaries(trackingId);
        // 참여 펫 목록 조회
        List<TrackingDetailSummary.ParticipatingPet> pets =
                trackingRepository.findParticipatingPetSummaries(trackingId);

        return TrackingDetailResponse.from(summary, images, pets);
    }

}
