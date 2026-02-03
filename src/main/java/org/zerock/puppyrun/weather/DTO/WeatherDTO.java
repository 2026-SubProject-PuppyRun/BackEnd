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
            String sky,  // 하늘
            String pty   // 강수
    ) {
    }
}
