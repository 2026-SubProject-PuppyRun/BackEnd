package org.zerock.puppyrun.weather.exception;

import org.zerock.puppyrun.common.exception.BusinessException;
import org.zerock.puppyrun.common.exception.ErrorCode;

public class InvalidWeatherException extends BusinessException {

    public InvalidWeatherException(String message) {
        super(ErrorCode.INVALID_WEATHER, message);
    }

    public InvalidWeatherException(String message, Throwable cause) {
        super(ErrorCode.INVALID_WEATHER, message, cause);
    }
}
