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
import org.zerock.puppyrun.pet.repository.PetRepository;
import org.zerock.puppyrun.tracking.controller.request.ChangeVisibilityRequest;
import org.zerock.puppyrun.tracking.controller.request.RegisterTrackingRequest;
import org.zerock.puppyrun.tracking.controller.request.UpdateTrackingRequest;
import org.zerock.puppyrun.tracking.DTO.UpdateTrackingDTO;
import org.zerock.puppyrun.tracking.controller.response.TrackingDetailResponse;
import org.zerock.puppyrun.tracking.entity.PetTracking;
import org.zerock.puppyrun.tracking.entity.Tracking;
import org.zerock.puppyrun.tracking.entity.TrackingPath;
import org.zerock.puppyrun.tracking.entity.Visibility;
import org.zerock.puppyrun.tracking.repository.PetTrackingRepository;
import org.zerock.puppyrun.tracking.repository.TrackingRepository;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class TrackingCommandService {
    private final DiaryRepository diaryRepository;
    private final TrackingRepository trackingRepository;
    private final MemberRepository memberRepository;

    private final PetTrackingRepository petTrackingRepository;
    private final PetRepository petRepository;

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
                .averagePace(request.averagePace())
//                .images(images.stream().map(MultipartFile::getOriginalFilename).toList()) todo: s3 저장 후 url 생성
                .path(path)
                .build();

        Tracking savedTracking = trackingRepository.save(tracking);

        // 펫-산책 정보 저장
        savePetTrackings(request.petIdList(), memberId, savedTracking);

    }

    /**
     * 산책 정보 수정
     */
    public TrackingDetailResponse updateTracking(UUID memberId, UUID trackingId, UpdateTrackingRequest request) {
        Tracking tracking = trackingRepository.findByIdAndVerifyOwnership(trackingId, memberId);

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
        Tracking tracking = trackingRepository.findByIdAndVerifyOwnership(trackingId, memberId);

        Visibility visibility = Visibility.from(request.visibility());
        // 공개 여부 변경
        tracking.changeVisibility(visibility);
    }

    /**
     * 산책 기록 삭제
     */
    public void deleteTracking(UUID memberId, UUID trackingId) {
        Tracking tracking = trackingRepository.findByIdAndVerifyOwnership(trackingId, memberId);

        // 연관된 일기가 있다면 tracking_id를 null로 변경
        diaryRepository.findByTrackingId(trackingId)
                .ifPresent(Diary::unsetTracking);
        trackingRepository.delete(tracking);
    }

    /**
     * 산책한 펫 리스트의 소유권을 검증하고 연관관계(PetTracking)를 일괄 저장합니다.
     */
    public void savePetTrackings(List<UUID> petIds, UUID memberId, Tracking tracking) {
        if (petIds == null || petIds.isEmpty()) {
            log.info("산책에 참여한 펫이 없습니다. 혼자 산책한 기록으로 유지됩니다. TrackingID: {}", tracking.getId());
            return;
        }
        List<PetTracking> petTrackingList = petIds.stream()
                .map(petId -> petRepository.findByIdAndVerifyOwnership(petId, memberId)) // 펫 조회 및 검증
                .map(pet -> new PetTracking(pet, tracking))                              // 연관관계 객체 생성
                .toList();

        petTrackingRepository.saveAll(petTrackingList); // 일괄 저장
    }
}
