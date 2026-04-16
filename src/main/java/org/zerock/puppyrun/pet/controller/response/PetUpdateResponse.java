package org.zerock.puppyrun.pet.controller.response;

import java.time.LocalDate;
import lombok.Builder;
import org.zerock.puppyrun.common.s3.support.S3Url;
import org.zerock.puppyrun.pet.entity.Pet;

@Builder
public record PetUpdateResponse(
        String name,

        LocalDate birthYear,

        Double weight,

        String color,

        Boolean isNeutered,

        String gender,

        @S3Url
        String profileImageUrl
) {
    public static PetUpdateResponse of(Pet pet) {
        return PetUpdateResponse.builder()
                .birthYear(pet.getBirthYear())
                .color(pet.getColor())
                .name(pet.getName())
                .weight(pet.getWeight())
                .isNeutered(pet.getIsNeutered())
                .gender(pet.getGender())
                .profileImageUrl(pet.getProfileImageUrl())
                .build();
    }
}
