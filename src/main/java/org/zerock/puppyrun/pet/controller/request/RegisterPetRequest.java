package org.zerock.puppyrun.pet.controller.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record RegisterPetRequest(
        @NotBlank(message = "강아지 이름은 필수입니다.")
        @Size(max = 50, message = "이름은 50자를 초과할 수 없습니다.")
        String name,

        @NotNull(message = "출생일은 필수입니다.")
        LocalDate birthYear,

        @NotBlank(message = "견종 코드는 필수입니다.")
        String breedCode,

        @NotNull(message = "중성화 여부는 필수입니다.")
        Boolean isNeutered,

        @NotBlank(message = "성별은 필수입니다.")
        @Pattern(regexp = "^[MF]$", message = "성별은 'M' 또는 'F'만 입력 가능합니다.")
        String gender,

        // null일 경우 Breed Enum의 기본값을 사용하도록 로직 처리됨
        String color,

        @Min(value = 1, message = "몸무게는 1kg 이상이어야 합니다.")
        Double weight
) {
}
