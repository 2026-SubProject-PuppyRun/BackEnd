package org.zerock.puppyrun.pet.controller.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record RegisterPetWeightLogRequest(
        @NotNull(message = "몸무게는 필수입니다.")
        @Min(value = 1, message = "몸무게는 1kg 이상이어야 합니다.")
        Double weight
) {
}
