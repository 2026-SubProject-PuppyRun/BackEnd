package org.zerock.puppyrun.pet.controller.response;

import java.time.LocalDate;
import java.util.UUID;
import lombok.Builder;
import org.zerock.puppyrun.common.s3.support.S3Url;
import org.zerock.puppyrun.pet.entity.Pet;
import org.zerock.puppyrun.pet.entity.PetBadge;

/**
 * 펫 상세 정보 응답 DTO
 */
@Builder
public record PetDetailResponse(
        UUID PetId,
        String name,
        LocalDate birthYear,
        Double weight,
        String color,
        String breedCode,
        
        @S3Url
        String profileImageUrl,
        Boolean isNeutered,
        String gender,
        BadgeInfo badgeInfo,
        String mbti
) {

    /**
     * Pet 엔티티를 PetDetailResponse DTO로 변환하는 정적 팩토리 메서드
     */
    public static PetDetailResponse of(Pet pet) {
        return PetDetailResponse.builder()
                .PetId(pet.getId())
                .name(pet.getName())
                .birthYear(pet.getBirthYear())
                .weight(pet.getWeight())
                .color(pet.getColor())
                .profileImageUrl(pet.getProfileImageUrl())
                .breedCode(pet.getBreed().getCode())
                .isNeutered(pet.getIsNeutered())
                .gender(pet.getGender())
                .badgeInfo(BadgeInfo.from(pet.getWalkedDistance()))
                .mbti(pet.getMbti())
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
        public static BadgeInfo from(int distance) {
            PetBadge badge = PetBadge.getBadgeByDistance(distance);
            return BadgeInfo.builder()
                    .code(badge.getCode())
                    .walkedDistance(distance)
                    .requiredDistance(badge.getRequiredDistance())
                    .nextRequiredDistance(badge.getNextRequiredDistance())
                    .build();
        }
    }
}
