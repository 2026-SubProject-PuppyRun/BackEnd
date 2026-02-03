package org.zerock.puppyrun.common.exception;

public class InvalidValueException extends BusinessException {
    public InvalidValueException(String message) {
        super(ErrorCode.INVALID_REQUEST, message);
    }

    public InvalidValueException(String message, Throwable cause) {
        super(ErrorCode.INVALID_REQUEST, message, cause);
    }
}
