package org.zerock.puppyrun.auth.oauth2.exception;

import org.zerock.puppyrun.common.exception.BusinessException;
import org.zerock.puppyrun.common.exception.ErrorCode;

/**
 * 소셜 제공자의 인가 코드, 액세스 토큰 또는 사용자 정보가 유효하지 않을 때 발생합니다.
 */
public class OAuth2AuthenticationException extends BusinessException {
    public OAuth2AuthenticationException(String message) {
        super(ErrorCode.USER_UNAUTHORIZED, message);
    }
}
