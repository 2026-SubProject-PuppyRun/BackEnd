package org.zerock.puppyrun.pet.DTO;

import java.time.LocalDate;
import lombok.Builder;

@Builder
public record UpdatePetDTO(
        String name,
        String color,
        Double weight,
        LocalDate birthYear,
        boolean isNeutered,
        String gender
) {
}
