package org.zerock.puppyrun.weather.exception;

import lombok.Getter;
import org.zerock.puppyrun.common.exception.ExternalApiParsingException;

/**
 * 기상청 API가 HTTP 응답 본문에 실패 코드를 반환한 경우 발생합니다.
 */
@Getter
public class WeatherApiResponseException extends ExternalApiParsingException {

    private final String responseCode;

    public WeatherApiResponseException(String responseCode, String responseMessage) {
        super(
                "기상청 API 응답 오류: resultCode=%s, resultMessage=%s"
                        .formatted(responseCode, responseMessage)
        );
        this.responseCode = responseCode;
    }
}
