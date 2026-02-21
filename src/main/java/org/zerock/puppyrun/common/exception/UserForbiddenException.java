package org.zerock.puppyrun.common.exception;

public class UserForbiddenException extends BusinessException {
    public UserForbiddenException(String message) {
        super(ErrorCode.USER_FORBIDDEN, message);
    }

    public UserForbiddenException(String message, Throwable cause) {
        super(ErrorCode.USER_FORBIDDEN, message, cause);
    }
}
