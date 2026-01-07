package org.zerock.puppyrun.member.exception;

import org.zerock.puppyrun.common.exception.BusinessException;
import org.zerock.puppyrun.common.exception.ErrorCode;

public class ExistingUserException extends BusinessException {
    public ExistingUserException(String message) {
        super(ErrorCode.EXISTS_USER, message);
    }

    public ExistingUserException(String message, Throwable cause) {
        super(ErrorCode.EXISTS_USER, message, cause);
    }
}
