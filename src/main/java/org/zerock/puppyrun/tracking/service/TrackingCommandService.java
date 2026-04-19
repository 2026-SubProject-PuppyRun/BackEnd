package org.zerock.puppyrun.tracking.service;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.zerock.puppyrun.common.exception.ResourceNotFoundException;
import org.zerock.puppyrun.common.exception.UserForbiddenException;
import org.zerock.puppyrun.common.s3.PathContext.TrackingPhotoContext;
import org.zerock.puppyrun.common.s3.S3Service;
import org.zerock.puppyrun.pet.entity.Pet;
import org.zerock.puppyrun.tracking.entity.PetTracking;
import org.zerock.puppyrun.tracking.repository.PetTrackingRepository;
import org.zerock.puppyrun.tracking.util.PaceConverter;
import org.zerock.puppyrun.diary.entity.Diary;
import org.zerock.puppyrun.diary.repository.DiaryRepository;
import org.zerock.puppyrun.member.entity.Member;
import org.zerock.puppyrun.member.repository.MemberRepository;
import org.zerock.puppyrun.pet.repository.PetRepository;
import org.zerock.puppyrun.tracking.controller.request.ChangeVisibilityRequest;
import org.zerock.puppyrun.tracking.controller.request.RegisterTrackingRequest;
import org.zerock.puppyrun.tracking.controller.request.RegisterTrackingRequest.restPeriods;
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
    private final PetTrackingRepository petTrackingRepository;
    private final PetRepository petRepository;

    // 이미지 업로드를 의존성 주입
    private final S3Service s3Service;


    /**
     * 산책 저장
     */
    public void saveTracking(UUID memberId, RegisterTrackingRequest request, List<MultipartFile> imageFiles) {
        Member member = memberRepository.findByIdOrThrow(memberId);

        UUID newTrackingId = UUID.randomUUID();
        LocalDate today = LocalDate.now();

        // 경로 데이터 변환
        List<TrackingPath> path = request.path().stream()
                .map(point -> new TrackingPath(point.lat(), point.lng(), point.time()))
                .toList();

        TrackingPath startPoint = path.getFirst();

        Integer restDuration = request.restPeriods().stream().mapToInt(restPeriods::durationSecond).sum();

        // 이미지 업로드
        List<String> imagesUrl = s3Service.uploadAll(imageFiles, new TrackingPhotoContext(newTrackingId, today));

        Tracking tracking = Tracking.builder()
                .id(newTrackingId)
                .member(member)
                .startedAt(request.startedAt())
                .endedAt(request.endedAt())
                .startedLat(startPoint.lat())
                .startedLng(startPoint.lng())
                .visibility(Visibility.from(request.visibility()))
                .distance(request.distance())
                .averagePace(PaceConverter.toDouble(request.averagePace()))
                .restDuration(restDuration)
                .images(imagesUrl)
                .path(path)
                .build();

        Tracking savedTracking = trackingRepository.save(tracking);

        // 산책과 강아지 매핑
        saveTrackingWithPets(member.getId(), request.petIdList(), savedTracking);
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

        List<String> images = List.copyOf(tracking.getImages());

        trackingRepository.delete(tracking);

        if (!images.isEmpty()) {
            s3Service.deleteAll(images);
        }
    }

    private void saveTrackingWithPets(UUID memberId, List<UUID> petIdList, Tracking savedTracking) {
        List<Pet> petList = petRepository.findAllById(petIdList);

        // 요청한 ID 리스트 개수와 DB에서 찾은 개수가 다르면 예외 발생 (존재하지 않는 펫 ID 포함됨)
        if (petList.size() != petIdList.size()) {
            throw new ResourceNotFoundException("요청한 강아지 중 존재하지 않는 강아지가 포함되어 있습니다.");
        }

        // 조회된 모든 강아지가 현재 로그인한 유저의 강아지가 맞는지 소유권 확인
        boolean hasNotOwnedPet = petList.stream()
                .anyMatch(pet -> pet.isNotOwner(memberId)); // Pet 엔티티의 isNotOwner 활용

        if (hasNotOwnedPet) {
            throw new UserForbiddenException("본인의 소유가 아닌 강아지는 산책에 등록할 수 없습니다.");
        }

        List<PetTracking> petTrackingList = petList.stream()
                .map(pet -> PetTracking.builder()
                        .pet(pet)
                        .tracking(savedTracking)
                        .build())
                .toList();

        petTrackingRepository.saveAll(petTrackingList);

    }
}
