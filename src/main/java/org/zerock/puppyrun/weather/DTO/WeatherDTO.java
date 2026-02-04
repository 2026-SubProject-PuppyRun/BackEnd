package org.zerock.puppyrun.weather.DTO;

import java.util.List;
import lombok.Builder;

@Builder
public record WeatherDTO(
        String date,
        String time,
        Detail detail
) {
    @Builder
    public record Detail(
            String temp, // 온도
            SkyType sky,  // 하늘
            PrecipitationType pty   // 강수
    ) {
    }
}
