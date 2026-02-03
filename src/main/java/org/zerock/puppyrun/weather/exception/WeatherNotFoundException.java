package org.zerock.puppyrun.weather.exception;

import org.zerock.puppyrun.common.exception.BusinessException;
import org.zerock.puppyrun.common.exception.ErrorCode;

public class WeatherNotFoundException extends BusinessException {
    public WeatherNotFoundException(String message) {
        super(ErrorCode.INVALID_WEATHER, message);
    }

    public WeatherNotFoundException(String message, Throwable cause) {
        super(ErrorCode.INVALID_WEATHER, message, cause);
    }
}
