package org.zerock.puppyrun.common.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;


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

        log.warn(
                "exceptionType={}, errorCode={}, status={}, uri={}, message={}",
                e.getClass().getSimpleName(),
                e.getErrorCode().getCode(),
                e.getErrorCode().getHttpStatus().value(),
                request.getRequestURI(),
                e.getMessage()
        );

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
     * Controller 메서드의 {@code @RequestParam}, {@code @PathVariable} 검증 실패를 처리합니다.
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolationException(
            ConstraintViolationException e,
            HttpServletRequest request) {
        String errorMessage = e.getConstraintViolations()
                .stream()
                .findFirst()
                .map(ConstraintViolation::getMessage)
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


    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingRequestParameter(
            MissingServletRequestParameterException e,
            HttpServletRequest request
    ) {
        ErrorCode errorCode = ErrorCode.INVALID_REQUEST;

        return ResponseEntity.status(errorCode.getHttpStatus())
                .body(ErrorResponse.of(
                        errorCode.getCode(),
                        errorCode.getDescription(),
                        "'" + e.getParameterName() + "' 파라미터는 필수입니다.",
                        request.getRequestURI()
                ));
    }


    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentTypeMismatch(
            MethodArgumentTypeMismatchException e,
            HttpServletRequest request
    ) {
        ErrorCode errorCode = ErrorCode.INVALID_REQUEST;

        String detailMessage = String.format("'%s' 파라미터의 값('%s') 형식이 올바르지 않습니다.",
                e.getName(), e.getValue());

        return ResponseEntity.status(errorCode.getHttpStatus())
                .body(ErrorResponse.of(
                        errorCode.getCode(),
                        errorCode.getDescription(),
                        detailMessage,
                        request.getRequestURI()
                ));
    }


    /**
     * JSON 파싱 실패 Request Body의 JSON 형식이 잘못되었거나, 필드 타입이 맞지 않을 때(예: Integer 필드에 "abc" 입력) 발생합니다.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadableException(
            HttpMessageNotReadableException e,
            HttpServletRequest request) {

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

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleHttpRequestMethodNotSupportedException(
            HttpRequestMethodNotSupportedException e,
            HttpServletRequest request
    ) {
        ErrorCode errorCode = ErrorCode.METHOD_NOT_ALLOWED;

        return ResponseEntity
                .status(errorCode.getHttpStatus())
                .body(ErrorResponse.of(
                        errorCode.getCode(),
                        errorCode.getDescription(),
                        e.getMessage(),
                        request.getRequestURI()
                ));
    }

    /**
     * 그 외 예상치 못한 모든 예외
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(
            Exception e,
            HttpServletRequest request) {

        log.error("Unhandled RuntimeException: ", e.getMessage(), e); // 스택 트레이스 전체 로깅
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
