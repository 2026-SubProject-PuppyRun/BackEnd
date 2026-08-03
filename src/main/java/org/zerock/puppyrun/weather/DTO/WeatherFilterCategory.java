package org.zerock.puppyrun.weather.DTO;

import lombok.Builder;

/**
 * 예보 유형별 기온·하늘 상태·강수 상태·강수량 코드 매핑입니다.
 */
@Builder
public record WeatherFilterCategory(
        String temp,
        String sky,
        String pty,
        String precipitationAmount
) {
}
