package org.zerock.puppyrun.diary.controller.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record UpdateDiaryRequest(
        @NotBlank(message = "일기 제목은 필수입니다.")
        @Size(max = 100, message = "제목은 100자를 초과할 수 없습니다.")
        String title,

        @NotBlank(message = "일기 내용은 필수입니다.")
        String content,

        @Valid
        @NotNull(message = "날씨 정보는 필수입니다.")
        RegisterDiaryRequest.Weather weather

) {
    public record Weather(
            @NotBlank(message = "기온 정보는 필수입니다.")
            String temp,

            @NotNull(message = "하늘 상태는 필수입니다.")
            String sky,

            @NotNull(message = "강수 형태는 필수입니다.")
            String pty
    ) {
    }
}
