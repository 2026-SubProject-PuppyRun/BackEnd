package org.zerock.puppyrun.common.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;


@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * 공통 BusinessException 처리
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> HandleBusinessException(
            BusinessException e,
            HttpServletRequest request) {

        log.warn("Business Exception: {}", e.getMessage());
        log.warn("Cause: ", e);

        ErrorResponse errorResponse = ErrorResponse.of(
                e.getErrorCode().getCode(),
                e.getDescription(),
                e.getMessage(),
                request.getRequestURI()
        );

        return ResponseEntity
                .status(e.getErrorCode().getHttpStatus())
                .body(errorResponse);
    }

    /**
     * MethodArgumentNotValidException 처리 (@Valid 검증 실패)
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException e,
            HttpServletRequest request) {
        log.warn("MethodArgumentNotValidException: {}", e.getMessage());

        // 첫 번째 validation 오류 메시지 추출
        String errorMessage = e.getBindingResult()
                .getFieldErrors()
                .stream()
                .findFirst()
                .map(DefaultMessageSourceResolvable::getDefaultMessage)
                .orElse("입력값이 올바르지 않습니다.");

        ErrorCode errorCode = ErrorCode.INVALID_REQUEST;

        ErrorResponse errorResponse = ErrorResponse.of(
                errorCode.getCode(),
                errorCode.getDescription(),
                errorMessage,
                request.getRequestURI()
        );

        return ResponseEntity
                .status(errorCode.getHttpStatus())
                .body(errorResponse);

    }


    /**
     * JSON 파싱 실패 Request Body의 JSON 형식이 잘못되었거나, 필드 타입이 맞지 않을 때(예: Integer 필드에 "abc" 입력) 발생합니다.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadableException(
            HttpMessageNotReadableException e,
            HttpServletRequest request) {

        log.warn("JSON Parsing Error: {}", e.getMessage());

        ErrorCode errorCode = ErrorCode.INVALID_REQUEST;

        String errorMessage = "요청 JSON 형식이 올바르지 않습니다. 오타나 데이터 타입을 확인해주세요.";

        ErrorResponse errorResponse = ErrorResponse.of(
                errorCode.getCode(),
                errorCode.getDescription(),
                errorMessage,
                request.getRequestURI()
        );

        return ResponseEntity
                .status(errorCode.getHttpStatus())
                .body(errorResponse);
    }


    /**
     * 그 외 예상치 못한 모든 예외
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(
            Exception e,
            HttpServletRequest request) {

        log.error("Unhandled RuntimeException: ", e); // 스택 트레이스 전체 로깅
        ErrorCode errorCode = ErrorCode.INTERNAL_SERVER_ERROR;

        ErrorResponse errorResponse = ErrorResponse.of(
                errorCode.getCode(),
                errorCode.getDescription(),
                e.getMessage(),
                request.getRequestURI()
        );
        return ResponseEntity
                .status(errorCode.getHttpStatus())
                .body(errorResponse);
    }
}
