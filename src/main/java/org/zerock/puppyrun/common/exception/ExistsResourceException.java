package org.zerock.puppyrun.common.exception;

public class ExistsResourceException extends BusinessException {
    public ExistsResourceException(String message) {
        super(ErrorCode.EXISTS_RESOURCE, message);
    }

    public ExistsResourceException(String message, Throwable cause) {
        super(ErrorCode.EXISTS_RESOURCE, message, cause);
    }
}
