package org.zerock.puppyrun.member.controller.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Builder;

@Builder
public record ChangePasswordRequest(
        @NotBlank(message = "기존 비밀번호는 필수입니다")
        @Size(min = 8, max = 20, message = "비밀번호는 8-20자 이내여야 합니다")
        @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[~!@#$%^&*()+|=])[A-Za-z\\d~!@#$%^&*()+|=]{8,20}$",
                message = "비밀번호는 영문, 숫자, 특수문자를 모두 포함해야 합니다")
        String oldPassword,

        @NotBlank(message = "새로운 비밀번호는 필수입니다")
        @Size(min = 8, max = 20, message = "비밀번호는 8-20자 이내여야 합니다")
        @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[~!@#$%^&*()+|=])[A-Za-z\\d~!@#$%^&*()+|=]{8,20}$",
                message = "비밀번호는 영문, 숫자, 특수문자를 모두 포함해야 합니다")
        String newPassword

) {
}
