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
        Integer distance,           // 산책 거리 (km)
        Integer durationMin,        // 산책 시간 (분)
        String averagePace,         // 산책 페이스

        DiaryDetail diary,          // 일기 작성 여부 (UI 뱃지용)
        List<String> trackingImages, // 산책 중 찍은 사진 리스트 (썸네일용)
        List<ParticipatingPet> participatingPets // 참여한 펫 목록
) {

    /**
     * DailyTracking과 pets를 조합하여 DailyPetTracking 객체를 생성합니다.
     */
    public static DailyPetTracking of(DailyTracking tracking, List<Pet> pets) {
        // 인자로 받은 pets 리스트를 ParticipatingPet 리스트로 변환
        List<ParticipatingPet> participatingPetsList = (pets == null)
                ? Collections.emptyList()
                : pets.stream()
                        .map(ParticipatingPet::from)
                        .toList();

        return new DailyPetTracking(
                tracking.trackingId(),
                tracking.startedAt(),
                tracking.endedAt(),
                (int) Math.round(tracking.distance() / 1000.0), // m -> km 단위 변환 후 반올림
                tracking.duration() / 60,                       // 초 -> 분 단위 변환
                tracking.averagePace(),
                DiaryDetail.from(tracking.diaryId()),
                tracking.trackingImages() != null ? tracking.trackingImages() : Collections.emptyList(),
                participatingPetsList
        );
    }

    @Builder
    public record DiaryDetail(
            boolean hasDiary,           // 일기 작성 여부 (UI 뱃지용)
            UUID diaryId               // 작성된 일기가 있다면 일기 ID (없으면 null)
    ) {
        public static DiaryDetail from(UUID diaryId) {
            return DiaryDetail.builder()
                    .hasDiary(diaryId != null)
                    .diaryId(diaryId)
                    .build();
        }
    }

    /**
     * 산책에 참여한 펫 정보
     */
    @Builder
    public record ParticipatingPet(
            UUID petId,                 // 펫 고유 ID
            String name,                // 펫 이름
            String profileImageUrl,     // 펫 프로필 이미지
            String themeColor           // 펫 고유 색상 (UI 테두리 등에 활용)
    ) {
        // Pet 엔티티를 받아 DTO로 변환하도록 수정
        public static ParticipatingPet from(Pet pet) {
            return ParticipatingPet.builder()
                    .petId(pet.getId())
                    .name(pet.getName())
                    .profileImageUrl(pet.getProfileImageUrl())
                    .themeColor(pet.getColor())
                    .build();
        }
    }
}
