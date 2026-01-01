package org.zerock.puppyrun.common.exception;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ErrorResponse {

    private final String code;
    private final String message;
    private final LocalDateTime timestamp;
    private final String path;

    public static ErrorResponse of(String code, String message, String path) {
        return ErrorResponse.builder()
                .code(code)
                .message(message)
                .timestamp(LocalDateTime.now())
                .path(path)
                .build();
    }
}
