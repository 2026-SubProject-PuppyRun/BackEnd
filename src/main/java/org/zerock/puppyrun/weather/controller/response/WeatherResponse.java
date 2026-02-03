package org.zerock.puppyrun.weather.controller.response;

import lombok.Builder;
import org.zerock.puppyrun.weather.DTO.RegionType;
import org.zerock.puppyrun.weather.DTO.WeatherDTO;
import org.zerock.puppyrun.weather.DTO.WeatherDTO.Detail;

@Builder
public record WeatherResponse(
        RegionType region,
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

    public static WeatherResponse of(WeatherDTO dto, RegionType region) {
        Detail detail = Detail.builder()
                .temp(dto.detail().temp())
                .sky(dto.detail().sky())
                .pty(dto.detail().pty())
                .build();
        return WeatherResponse.builder()
                .detail(detail)
                .region(region)
                .date(dto.date())
                .time(dto.time())
                .build();
    }
}
