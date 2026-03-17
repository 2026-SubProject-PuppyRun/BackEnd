package org.zerock.puppyrun.pet.controller.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record UpdatePetRequest(
        @NotBlank(message = "강아지 이름은 필수입니다.")
        @Size(max = 50, message = "이름은 50자를 초과할 수 없습니다.")
        String name,

        @NotNull(message = "출생일은 필수입니다.")
        LocalDate birthYear,

        @NotNull(message = "몸무게는 필수입니다.")
        @Min(value = 1, message = "몸무게는 1kg 이상이어야 합니다.")
        Double weight,

        @NotNull(message = "중성화 여부는 필수입니다.")
        Boolean isNeutered,

        @NotBlank(message = "성별은 필수입니다.")
        @Pattern(regexp = "^[MF]$", message = "성별은 'M' 또는 'F'만 입력 가능합니다.")
        String gender,

        @NotBlank(message = "색상 코드는 필수입니다.")
        String color
) {
}
