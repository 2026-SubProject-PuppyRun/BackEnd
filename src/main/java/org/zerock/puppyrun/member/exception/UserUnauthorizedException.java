package org.zerock.puppyrun.member.exception;

import org.zerock.puppyrun.common.exception.BusinessException;
import org.zerock.puppyrun.common.exception.ErrorCode;

public class UserUnauthorizedException extends BusinessException {
    public UserUnauthorizedException(String message) {
        super(ErrorCode.USER_UNAUTHORIZED, message);
    }

    public UserUnauthorizedException(String message, Throwable cause) {
        super(ErrorCode.USER_UNAUTHORIZED, message, cause);
    }
}
