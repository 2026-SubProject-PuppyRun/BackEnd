package org.zerock.puppyrun.common.exception;

public class DataIntegrityException extends BusinessException {
    public DataIntegrityException(String message) {
        super(ErrorCode.DATA_INTEGRITY_VIOLATION, message);
    }

    public DataIntegrityException(String message, Throwable cause) {
        super(ErrorCode.DATA_INTEGRITY_VIOLATION, message, cause);
    }
}
