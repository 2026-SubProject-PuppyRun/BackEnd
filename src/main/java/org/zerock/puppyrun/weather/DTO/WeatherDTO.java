package org.zerock.puppyrun.weather.DTO;

import java.util.List;
import lombok.Builder;

@Builder
public record WeatherDTO(
        List<WeatherList> weatherList
) {
    @Builder
    public record WeatherList(
            String date,
            String time,
            String temp, // 온도
            SkyType sky,  // 하늘
            PrecipitationType pty, // 강수 상태
            String pcp // 강수량
    ) {
    }
}
