package org.zerock.puppyrun.common.exception;

public class CacheNotFoundException extends BusinessException {
    public CacheNotFoundException(String message) {
        super(ErrorCode.CACHE_NOT_FOUND, message);
    }

    public CacheNotFoundException(String message, Throwable cause) {
        super(ErrorCode.CACHE_NOT_FOUND, message, cause);
    }
}
