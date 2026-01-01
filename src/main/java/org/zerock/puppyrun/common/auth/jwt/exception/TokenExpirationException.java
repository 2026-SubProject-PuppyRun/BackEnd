package org.zerock.puppyrun.common.auth.jwt.exception;

import org.zerock.puppyrun.common.exception.BusinessException;
import org.zerock.puppyrun.common.exception.ErrorCode;

public class TokenExpirationException extends BusinessException {
    public TokenExpirationException(String message) {
        super(ErrorCode.TOKEN_EXPIRED, message);
    }

    public TokenExpirationException(String message, Throwable cause) {
        super(ErrorCode.TOKEN_EXPIRED, message, cause);
    }
}
