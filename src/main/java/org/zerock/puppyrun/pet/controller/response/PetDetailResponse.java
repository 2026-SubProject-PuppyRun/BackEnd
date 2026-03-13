package org.zerock.puppyrun.pet.controller.response;

import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Builder;
import org.zerock.puppyrun.pet.entity.Breed;
import org.zerock.puppyrun.pet.entity.Pet;
import org.zerock.puppyrun.pet.entity.PetBadge;

/**
 * 펫 상세 정보 응답 DTO
 */
@Builder
public record PetDetailResponse(
        UUID PetId,
        String name,
        LocalDateTime birthYear,
        Double weight,
        String color,
        String breedCode,
        String profileImageUrl,
        BadgeInfo badgeInfo
) {

    /**
     * Pet 엔티티를 PetDetailResponse DTO로 변환하는 정적 팩토리 메서드
     */
    public static PetDetailResponse of(Pet pet, int walkedDistance) {
        return PetDetailResponse.builder()
                .PetId(pet.getId())
                .name(pet.getName())
                .birthYear(pet.getBirthYear())
                .weight(pet.getWeight())
                .color(pet.getColor())
                .profileImageUrl(pet.getProfileImageUrl())
                .breedCode(pet.getBreed().getCode())
                .badgeInfo(BadgeInfo.from(pet.getBadge(), walkedDistance))
                .build();
    }


    /**
     * 뱃지 상세 정보
     */
    @Builder
    public record BadgeInfo(
            String code,
            int walkedDistance,
            int requiredDistance,
            int nextRequiredDistance
    ) {
        public static BadgeInfo from(PetBadge badge, int distance) {
            return BadgeInfo.builder()
                    .code(badge.getCode())
                    .walkedDistance(distance)
                    .requiredDistance(badge.getRequiredDistance())
                    .nextRequiredDistance(badge.getNextRequiredDistance())
                    .build();
        }
    }
}
