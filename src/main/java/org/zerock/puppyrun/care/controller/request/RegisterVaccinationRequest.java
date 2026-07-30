package org.zerock.puppyrun.care.controller.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record RegisterVaccinationRequest(
        @NotBlank(message = "접종명은 필수입니다.")
        @Size(max = 100, message = "접종명은 100자를 초과할 수 없습니다.")
        String vaccineName,

        @NotNull(message = "접종일은 필수입니다.")
        LocalDate vaccinatedAt,

        LocalDate nextVaccinationDate,

        @Size(max = 100, message = "병원명은 100자를 초과할 수 없습니다.")
        String hospitalName,

        String memo
) {
}
