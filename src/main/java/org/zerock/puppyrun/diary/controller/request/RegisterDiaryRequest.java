package org.zerock.puppyrun.diary.controller.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record RegisterDiaryRequest(

        @NotNull(message = "산책 기록 ID는 필수입니다.")
        UUID trackingId,

        @NotNull(message = "일기 작성 시간대 정보는 필수입니다.")
        LocalDateTime writingTime,

        @NotBlank(message = "일기 제목은 필수입니다.")
        @Size(max = 100, message = "제목은 100자를 초과할 수 없습니다.")
        String title,

        @NotBlank(message = "일기 내용은 필수입니다.")
        String content,

        @Valid
        @NotNull(message = "날씨 정보는 필수입니다.")
        Weather weather,

        // 이미지는 빈 리스트가 올 수 있으므로 별도의 @NotEmpty 검증을 하지 않음
        List<String> images
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
