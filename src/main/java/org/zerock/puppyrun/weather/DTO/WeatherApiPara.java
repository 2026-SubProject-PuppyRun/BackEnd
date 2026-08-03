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
}
