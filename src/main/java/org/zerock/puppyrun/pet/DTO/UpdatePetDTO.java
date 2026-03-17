package org.zerock.puppyrun.pet.DTO;

import lombok.Builder;
import org.zerock.puppyrun.pet.entity.Breed;

@Builder
public record UpdatePetDTO(
        String name,
        String color,
        Double weight,
        boolean isNeutered,
        String gender
) {
}
