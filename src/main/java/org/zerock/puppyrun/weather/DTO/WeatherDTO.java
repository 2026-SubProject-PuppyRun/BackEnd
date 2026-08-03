package org.zerock.puppyrun.weather.DTO;

import java.util.List;
import lombok.Builder;

@Builder
public record WeatherDTO(
        String date,
        List<Detail> detail
) {
    @Builder
    public record Detail(
            String time,
            String temp, // 온도
            SkyType sky,  // 하늘
            PrecipitationType pty, // 강수 상태
            String precipitationAmount // 강수량
    ) {
    }
}
