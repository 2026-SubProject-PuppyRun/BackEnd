package org.zerock.puppyrun.common.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

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

    @Test
    @DisplayName("존재하지 않는 리소스 요청은 404 응답으로 변환한다")
    void handleNoResourceFoundException() {
        // given
        HttpServletRequest request = mock(HttpServletRequest.class);
        given(request.getRequestURI()).willReturn("/api/not-found");
        NoResourceFoundException exception = new NoResourceFoundException(HttpMethod.GET, "api/not-found");

        // when
        ResponseEntity<ErrorResponse> response = globalExceptionHandler
                .handleNoResourceFoundException(exception, request);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody())
                .extracting(ErrorResponse::getCode, ErrorResponse::getMessage, ErrorResponse::getPath)
                .containsExactly("CLIENT_002", "요청한 리소스를 찾을 수 없습니다.", "/api/not-found");
    }

    @Test
    @DisplayName("지원하지 않는 Content-Type 요청은 415 응답으로 변환한다")
    void handleHttpMediaTypeNotSupportedException() {
        // given
        HttpServletRequest request = mock(HttpServletRequest.class);
        given(request.getRequestURI()).willReturn("/api/diaries");
        HttpMediaTypeNotSupportedException exception = new HttpMediaTypeNotSupportedException(
                MediaType.MULTIPART_FORM_DATA,
                List.of(MediaType.APPLICATION_JSON),
                HttpMethod.POST
        );

        // when
        ResponseEntity<ErrorResponse> response = globalExceptionHandler
                .handleHttpMediaTypeNotSupportedException(exception, request);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNSUPPORTED_MEDIA_TYPE);
        assertThat(response.getBody())
                .extracting(ErrorResponse::getCode, ErrorResponse::getMessage, ErrorResponse::getPath)
                .containsExactly("CLIENT_005", "지원하지 않는 Content-Type입니다.", "/api/diaries");
    }
}
