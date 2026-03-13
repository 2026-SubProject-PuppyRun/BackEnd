package org.zerock.puppyrun.pet.controller.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

public record UpdatePetRequest(
        @NotBlank(message = "강아지 이름은 필수입니다.")
        @Size(max = 50, message = "이름은 50자를 초과할 수 없습니다.")
        String name,

        @NotNull(message = "출생일은 필수입니다.")
        LocalDateTime birthYear,

        @NotNull(message = "몸무게는 필수입니다.")
        Double weight,

        @NotBlank(message = "색상 코드는 필수입니다.")
        String color,

        @NotBlank(message = "프로필 이미지 URL은 필수입니다.")
        String profileImageUrl
) {
}
