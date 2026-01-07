package org.zerock.puppyrun.common.exception;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ErrorResponse {

    private final String code;
    private final String description;
    private final String message;
    private final LocalDateTime timestamp;
    private final String path;

    public static ErrorResponse of(String code, String description, String message, String path) {
        return ErrorResponse.builder()
                .code(code)
                .description(description)
                .message(message)
                .timestamp(LocalDateTime.now())
                .path(path)
                .build();
    }
}
