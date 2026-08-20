package org.zerock.puppyrun.common.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler globalExceptionHandler = new GlobalExceptionHandler();

    @Test
    @DisplayName("내부 DTO 불변식 검증 실패는 서버 오류 응답으로 변환한다")
    void handleIllegalArgumentException() {
        // given
        HttpServletRequest request = mock(HttpServletRequest.class);
        given(request.getRequestURI()).willReturn("/api/oauth2/kakao/sign-in");
        IllegalArgumentException exception = new IllegalArgumentException("소셜 이메일 제공 동의가 필요합니다.");

        // when
        ResponseEntity<ErrorResponse> response = globalExceptionHandler
                .handleIllegalArgumentException(exception, request);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody())
                .extracting(ErrorResponse::getCode, ErrorResponse::getMessage, ErrorResponse::getPath)
                .containsExactly("SERVER_001", "소셜 이메일 제공 동의가 필요합니다.", "/api/oauth2/kakao/sign-in");
    }
}
