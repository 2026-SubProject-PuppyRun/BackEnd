package org.zerock.puppyrun.tracking.service;

import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.zerock.puppyrun.common.exception.ResourceNotFoundException;
import org.zerock.puppyrun.common.exception.UserForbiddenException;
import org.zerock.puppyrun.diary.entity.Diary;
import org.zerock.puppyrun.diary.repository.DiaryRepository;
import org.zerock.puppyrun.member.entity.Member;
import org.zerock.puppyrun.member.repository.MemberRepository;
import org.zerock.puppyrun.tracking.controller.request.ChangeVisibilityRequest;
import org.zerock.puppyrun.tracking.controller.request.RegisterTrackingRequest;
import org.zerock.puppyrun.tracking.controller.response.MainTrackingResponse;
import org.zerock.puppyrun.tracking.controller.request.UpdateTrackingRequest;
import org.zerock.puppyrun.tracking.DTO.UpdateTrackingDTO;
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
    private final DiaryRepository diaryRepository;
    private final TrackingRepository trackingRepository;
    private final MemberRepository memberRepository;

    /**
     * 산책 상세 조회
     */
    public Tracking findTrackingWithOwnershipCheck(UUID memberId, UUID trackingId) {
        Tracking tracking = trackingRepository.findById(trackingId)
                .orElseThrow(() -> new ResourceNotFoundException("해당 산책 기록을 찾을 수 없습니다."));

        if (tracking.isNotOwner(memberId)) {
            throw new UserForbiddenException("해당 산책 기록에 대한 권한이 없습니다.");
        }

        return tracking;
    }

    /**
     * 산책 저장
     */
    @Transactional
    public void saveTracking(UUID memberId, RegisterTrackingRequest request, List<MultipartFile> images) {
        Member member = memberRepository.findByIdOrThrow(memberId);

        // 경로 데이터 변환
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
//                .images(images.stream().map(MultipartFile::getOriginalFilename).toList()) todo: s3 저장 후 url 생성
                .path(path)
                .build();

        trackingRepository.save(tracking);
    }

    /**
     * 산책 정보 수정
     */
    @Transactional
    public TrackingDetailResponse updateTracking(UUID memberId, UUID trackingId, UpdateTrackingRequest request) {
        Tracking tracking = findTrackingWithOwnershipCheck(memberId, trackingId);

        UpdateTrackingDTO updateTrackingDTO = UpdateTrackingDTO.builder()
                .endedAt(request.endedAt())
                .startedAt(request.startedAt())
                .visibility(request.visibility())
                .build();

        tracking.update(updateTrackingDTO);

        // 일기 ID 조회 (없으면 null)
        UUID diaryId = diaryRepository.findIdByTrackingId(trackingId)
                .orElse(null);

        return TrackingDetailResponse.of(tracking, diaryId);
    }

    /**
     * 산책 공개 여부 변경 (Lightweight Update)
     */
    @Transactional
    public void changeTrackingVisibility(UUID memberId, UUID trackingId, ChangeVisibilityRequest request) {
        Tracking tracking = findTrackingWithOwnershipCheck(memberId, trackingId);
        Visibility visibility = Visibility.from(request.visibility());
        // 공개 여부 변경
        tracking.changeVisibility(visibility);
    }

    /**
     * 산책 기록 삭제
     */
    @Transactional
    public void deleteTracking(UUID memberId, UUID trackingId) {
        Tracking tracking = findTrackingWithOwnershipCheck(memberId, trackingId);

        // 연관된 일기가 있다면 tracking_id를 null로 변경
        diaryRepository.findByTrackingId(trackingId)
                .ifPresent(Diary::unsetTracking);
        trackingRepository.delete(tracking);
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
        Tracking tracking = findTrackingWithOwnershipCheck(memberId, trackingId);

        UUID diaryId = diaryRepository.findIdByTrackingId(trackingId)
                .orElse(null);

        return TrackingDetailResponse.of(tracking, diaryId);
    }
}
