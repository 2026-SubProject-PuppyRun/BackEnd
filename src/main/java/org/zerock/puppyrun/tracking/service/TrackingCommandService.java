package org.zerock.puppyrun.tracking.service;

import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.zerock.puppyrun.diary.entity.Diary;
import org.zerock.puppyrun.diary.repository.DiaryRepository;
import org.zerock.puppyrun.member.entity.Member;
import org.zerock.puppyrun.member.repository.MemberRepository;
import org.zerock.puppyrun.tracking.controller.request.ChangeVisibilityRequest;
import org.zerock.puppyrun.tracking.controller.request.RegisterTrackingRequest;
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
@Transactional
public class TrackingCommandService {
    private final DiaryRepository diaryRepository;
    private final TrackingRepository trackingRepository;
    private final MemberRepository memberRepository;
    private final TrackingVerification trackingVerification;

    /**
     * 산책 저장
     */
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

        Tracking savedTracking = trackingRepository.save(tracking);

    }

    /**
     * 산책 정보 수정
     */
    public TrackingDetailResponse updateTracking(UUID memberId, UUID trackingId, UpdateTrackingRequest request) {
        Tracking tracking = trackingVerification.ownershipCheck(memberId, trackingId);

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
    public void changeTrackingVisibility(UUID memberId, UUID trackingId, ChangeVisibilityRequest request) {
        Tracking tracking = trackingVerification.ownershipCheck(memberId, trackingId);
        Visibility visibility = Visibility.from(request.visibility());
        // 공개 여부 변경
        tracking.changeVisibility(visibility);
    }

    /**
     * 산책 기록 삭제
     */
    public void deleteTracking(UUID memberId, UUID trackingId) {
        Tracking tracking = trackingVerification.ownershipCheck(memberId, trackingId);

        // 연관된 일기가 있다면 tracking_id를 null로 변경
        diaryRepository.findByTrackingId(trackingId)
                .ifPresent(Diary::unsetTracking);
        trackingRepository.delete(tracking);
    }
}
