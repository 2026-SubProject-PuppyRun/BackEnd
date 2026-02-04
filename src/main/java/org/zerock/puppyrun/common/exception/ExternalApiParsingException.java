package org.zerock.puppyrun.common.exception;

/**
 * 외부 API 요청이 올바르지 않을 때
 */
public class ExternalApiParsingException extends BusinessException {
    public ExternalApiParsingException(String message) {
        super(ErrorCode.EXTERNAL_API_ERROR, message);
    }

    public ExternalApiParsingException(String message, Throwable cause) {
        super(ErrorCode.EXTERNAL_API_ERROR, message, cause);
    }
}
