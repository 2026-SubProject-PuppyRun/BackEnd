package org.zerock.puppyrun.common.auth.jwt.exception;

import org.zerock.puppyrun.common.exception.BusinessException;
import org.zerock.puppyrun.common.exception.ErrorCode;

public class NotFoundTokenException extends BusinessException {
    public NotFoundTokenException(String message) {
        super(ErrorCode.NOT_EXISTS_TOKEN, message);
    }

    public NotFoundTokenException(String message, Throwable cause) {
        super(ErrorCode.NOT_EXISTS_TOKEN, message, cause);
    }

}
