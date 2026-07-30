package org.zerock.puppyrun.care.controller.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

public record RegisterMedicationRequest(
        @NotBlank(message = "약 이름은 필수입니다.")
        @Size(max = 100, message = "약 이름은 100자를 초과할 수 없습니다.")
        String medicationName,

        @NotNull(message = "투약 시각은 필수입니다.")
        LocalDateTime administeredAt,

        @NotNull(message = "투약량은 필수입니다.")
        @DecimalMin(value = "0.0", inclusive = false, message = "투약량은 0보다 커야 합니다.")
        Double doseAmount,

        @NotBlank(message = "투약 단위는 필수입니다.")
        @Size(max = 30, message = "투약 단위는 30자를 초과할 수 없습니다.")
        String doseUnit,

        String memo
) {
}
