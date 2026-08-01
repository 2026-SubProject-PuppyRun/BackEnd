package org.zerock.puppyrun.pet.controller.request;

import jakarta.validation.constraints.NotBlank;

public record UpdateMbtiRequest(
        @NotBlank String mbti
) {
}
