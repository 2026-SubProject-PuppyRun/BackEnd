package org.zerock.puppyrun.auth.controller.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

@Builder
public record SignInRequest(
        @Email(message = "이메일 형식이 올바르지 않습니다.")
        @NotBlank(message = "이메일은 필수입니다.")
        String email,

        @NotBlank(message = "비밀번호는 필수입니다.")
        String password
) {
}
