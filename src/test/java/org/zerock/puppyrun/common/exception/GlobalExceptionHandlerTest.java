package org.zerock.puppyrun.common.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler globalExceptionHandler = new GlobalExceptionHandler();

    @Test
    @DisplayName("요청 파라미터 검증 실패를 CLIENT_001 응답으로 변환한다")
    void handlesConstraintViolationException() {
        ConstraintViolation<?> violation = mock(ConstraintViolation.class);
        when(violation.getMessage()).thenReturn("위도는 90 이하여야 합니다.");
        ConstraintViolationException exception = new ConstraintViolationException(Set.of(violation));
        MockHttpServletRequest request = new MockHttpServletRequest(
                "GET",
                "/api/tracking/recommendations"
        );

        ResponseEntity<ErrorResponse> response = globalExceptionHandler
                .handleConstraintViolationException(exception, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo("CLIENT_001");
        assertThat(response.getBody().getDescription()).isEqualTo("잘못된 요청입니다.");
        assertThat(response.getBody().getMessage()).isEqualTo("위도는 90 이하여야 합니다.");
        assertThat(response.getBody().getPath()).isEqualTo("/api/tracking/recommendations");
    }
}
