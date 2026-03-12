package org.zerock.puppyrun.pet.controller.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

public record RegisterPetRequest(
        @NotBlank(message = "강아지 이름은 필수입니다.")
        @Size(max = 50, message = "이름은 50자를 초과할 수 없습니다.")
        String name,

        @NotNull(message = "출생일은 필수입니다.")
        LocalDateTime birthYear,

        @NotBlank(message = "견종 코드는 필수입니다.")
        String breedCode,

        // null일 경우 Breed Enum의 기본값을 사용하도록 로직 처리됨
        String color,
        Double weight
) {
}
