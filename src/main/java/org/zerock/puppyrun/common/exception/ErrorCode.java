package org.zerock.puppyrun.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;


@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    // 일반적인 에러
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "SERVER_001", "서버 내부 오류가 발생했습니다."),
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "SERVER_002", "잘못된 요청입니다."),
    DATA_INTEGRITY_VIOLATION(HttpStatus.INTERNAL_SERVER_ERROR, "SERVER_003", "데이터 정합성에 문제가 발생했습니다."),

    // 인증/인가 관련 에러
    USER_UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "AUTH_001", "로그인 정보가 유효하지 않습니다."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH_002", "유효하지 않은 토큰입니다."),
    MISSING_AUTHORIZATION_HEADER(HttpStatus.UNAUTHORIZED, "AUTH_003", "Authorization 헤더가 없거나 Bearer 토큰 형식이 아닙니다."),
    NOT_EXISTS_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH_004", "토큰이 존재하지 않습니다."),
    TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "AUTH_005", "토큰이 만료되었습니다."),
    USER_FORBIDDEN(HttpStatus.FORBIDDEN, "AUTH_006", "접근 권한이 없습니다."),

    // cache 에러
    CACHE_NOT_FOUND(HttpStatus.INTERNAL_SERVER_ERROR, "CACHE_001", "캐시를 찾을 수 없습니다."),

    // 외부 API 에러
    EXTERNAL_API_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "API_001", "외부 API 요청 중 오류가 발생했습니다."),

    // 날씨 에러
    INVALID_WEATHER(HttpStatus.INTERNAL_SERVER_ERROR, "WEATHER_001", "잘못된 날씨입니다."),

    // 유저 에러
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER_001", "유저를 찾을 수 없습니다."),
    EXISTS_USER(HttpStatus.CONFLICT, "USER_002", "이미 존재하는 유저입니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String description;
}
