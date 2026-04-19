package org.zerock.puppyrun.care.controller.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record RegisterAllergyRequest(
        @NotBlank(message = "알러지 원인명은 필수입니다.")
        @Size(max = 100, message = "알러지 원인명은 100자를 초과할 수 없습니다.")
        String allergenName,

        @Size(max = 255, message = "증상은 255자를 초과할 수 없습니다.")
        String symptom,

        String severity,

        LocalDate identifiedAt,

        @NotNull(message = "활성 여부는 필수입니다.")
        Boolean isActive,

        String memo
) {
}
