package org.zerock.puppyrun.pet.controller.response;


import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.Builder;
import org.zerock.puppyrun.common.s3.support.S3Url;
import org.zerock.puppyrun.pet.entity.Pet;
import org.zerock.puppyrun.pet.entity.PetBadge;

public record PetListResponse(
        int totalPetCount,
        List<PetSummary> petSummaryList

) {

    public static PetListResponse of(List<Pet> pets) {
        return of(pets, Map.of());
    }

    public static PetListResponse of(List<Pet> pets, Map<UUID, Integer> walkedDistances) {
        List<PetSummary> summaryList = pets.stream()
                .map(pet -> PetSummary.from(pet, walkedDistances.getOrDefault(pet.getId(), pet.getWalkedDistance())))
                .toList();

        return new PetListResponse(summaryList.size(), summaryList);
    }

    @Builder
    public record PetSummary(
            UUID PetId,
            String name,
            LocalDate birthYear,
            Double weight,
            String color,

            @S3Url
            String profileImageUrl,
            String breedCode,
            String badgeCode,
            String gender,
            String mbti,
            boolean isNeutered
    ) {
        public static PetSummary from(Pet pet) {
            return from(pet, pet.getWalkedDistance());
        }

        public static PetSummary from(Pet pet, int walkedDistance) {
            PetBadge badge = PetBadge.getBadgeByDistance(walkedDistance);

            return PetSummary.builder()
                    .PetId(pet.getId())
                    .name(pet.getName())
                    .birthYear(pet.getBirthYear())
                    .weight(pet.getWeight())
                    .color(pet.getColor())
                    .profileImageUrl(pet.getProfileImageUrl())
                    .breedCode(pet.getBreed().getCode())
                    .badgeCode(badge.getCode())
                    .gender(pet.getGender())
                    .mbti(pet.getMbti())
                    .isNeutered(pet.getIsNeutered())
                    .build();
        }

    }
}
