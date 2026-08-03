package org.zerock.puppyrun.weather.controller.response;

import java.util.List;
import lombok.Builder;
import org.zerock.puppyrun.weather.DTO.WeatherDTO;
import org.zerock.puppyrun.weather.DTO.WeatherRegion;

@Builder
public record WeatherResponse(
        List<String> region,
        String date,
        String time,
        Detail detail
) {
    @Builder
    public record Detail(
            String temp, // 온도
            String sky,  // 하늘
            String pty,  // 강수 상태
            String pcp // 강수량
    ) {
    }

    public static WeatherResponse of(WeatherDTO dto, WeatherRegion region) {
        WeatherDTO.WeatherList weather = dto.weatherList().getFirst();
        Detail detail = Detail.builder()
                .temp(weather.temp())
                .sky(weather.sky().getCode())
                .pty(weather.pty().getCode())
                .pcp(weather.pcp())
                .build();
        return WeatherResponse.builder()
                .detail(detail)
                .region(region.names())
                .date(weather.date())
                .time(weather.time())
                .build();
    }
}
