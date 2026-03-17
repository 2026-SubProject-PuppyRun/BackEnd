package org.zerock.puppyrun.pet.controller.response;


import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.Builder;
import org.zerock.puppyrun.pet.entity.Breed;
import org.zerock.puppyrun.pet.entity.Pet;

public record PetListResponse(
        int totalPetCount,
        List<PetSummary> petSummaryList

) {

    public static PetListResponse of(List<Pet> pets) {
        List<PetSummary> summaryList = pets.stream()
                .map(PetSummary::from)
                .toList();

        return new PetListResponse(summaryList.size(), summaryList);
    }

    @Builder
    public record PetSummary(
            UUID PetId,
            String name,
            LocalDateTime birthYear,
            Double weight,
            String color,
            String profileImageUrl,
            String breedCode,
            String badgeCode
    ) {
        public static PetSummary from(Pet pet) {
            return PetSummary.builder()
                    .PetId(pet.getId())
                    .name(pet.getName())
                    .birthYear(pet.getBirthYear())
                    .weight(pet.getWeight())
                    .color(pet.getColor())
                    .profileImageUrl(pet.getProfileImageUrl())
                    .breedCode(pet.getBreed().getCode())
                    .badgeCode(pet.getBadge().getCode())
                    .build();
        }

    }
}
