package org.zerock.puppyrun.statistics.DTO;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import lombok.Builder;
import org.zerock.puppyrun.pet.entity.Pet;
import org.zerock.puppyrun.tracking.DTO.DailyTracking;

public record DailyPetTracking(
        UUID trackingId,            // 산책 고유 ID (상세 페이지 이동용)
        LocalDateTime startedAt,    // 산책 시작 시간
        LocalDateTime endedAt,      // 산책 종료 시간
        Integer distance,           // 산책 거리 (m)
        Integer duration,           // 산책 시간 (초)
        Double averagePace,         // 산책 페이스
        DiaryDetail diary,          // 일기 작성 여부 (UI 뱃지용)
        List<TrackingImageSummary> trackingImages, // 산책 중 찍은 사진 리스트 (썸네일용)
        List<ParticipatingPet> participatingPets // 참여한 펫 목록
) {

    public static DailyPetTracking of(DailyTracking tracking, List<Pet> pets) {
        return new DailyPetTracking(
                tracking.trackingId(),
                tracking.startedAt(),
                tracking.endedAt(),
                tracking.distance(),
                tracking.duration(),
                tracking.averagePace(),
                DiaryDetail.from(tracking.diaryId()),
                TrackingImageSummary.from(tracking.trackingImages()),
                ParticipatingPet.from(pets)
        );
    }

    @Builder
    public record DiaryDetail(
            boolean hasDiary,
            UUID diaryId
    ) {
        public static DiaryDetail from(UUID diaryId) {
            return new DiaryDetail(
                    diaryId != null,
                    diaryId
            );
        }
    }

    @Builder
    public record TrackingImageSummary(
            Integer order,
            String image
    ) {
        public static List<TrackingImageSummary> from(
                List<DailyTracking.TrackingImageSummary> images
        ) {
            return (images == null) ? Collections.emptyList()
                    : images.stream()
                            .map(i -> new TrackingImageSummary(i.order(), i.image()))
                            .toList();
        }
    }

    @Builder
    public record ParticipatingPet(
            UUID petId,
            String name,
            String profileImageUrl,
            String themeColor
    ) {
        public static List<ParticipatingPet> from(List<Pet> pets) {
            return (pets == null) ? Collections.emptyList()
                    : pets.stream()
                            .map(pet -> new ParticipatingPet(
                                    pet.getId(),
                                    pet.getName(),
                                    pet.getProfileImageUrl(),
                                    pet.getColor()
                            ))
                            .toList();
        }
    }
}
