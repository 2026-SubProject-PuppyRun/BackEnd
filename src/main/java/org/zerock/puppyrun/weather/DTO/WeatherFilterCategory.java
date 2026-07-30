package org.zerock.puppyrun.weather.DTO;

import lombok.Builder;

/**
 * 예보 유형별 온도·하늘·강수 코드 매핑입니다.
 */
@Builder
public record WeatherFilterCategory(
        String temp,
        String sky,
        String pty
) {
}
