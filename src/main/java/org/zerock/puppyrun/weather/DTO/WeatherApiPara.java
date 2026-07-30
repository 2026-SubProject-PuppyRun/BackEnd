package org.zerock.puppyrun.weather.DTO;

import lombok.Builder;

/**
 * 기상청 예보 API 요청 파라미터입니다.
 */
@Builder
public record WeatherApiPara(
        String baseDate,
        String baseTime,
        int nx,
        int ny,
        int pageNo,
        int numOfRows,
        String path
) {

    /**
     * 기존 초단기예보 호출과 호환되는 생성자입니다.
     */
    public WeatherApiPara(String baseDate, String baseTime, int nx, int ny) {
        this(baseDate, baseTime, nx, ny, 1, 100, "/getUltraSrtFcst");
    }
}
