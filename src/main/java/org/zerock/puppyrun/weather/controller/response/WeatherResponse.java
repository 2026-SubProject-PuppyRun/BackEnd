package org.zerock.puppyrun.weather.controller.response;

import lombok.Builder;
import org.zerock.puppyrun.weather.DTO.RegionType;
import org.zerock.puppyrun.weather.DTO.WeatherDTO;

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
            String pty,  // 강수 상태
            String precipitationAmount // 강수량
    ) {
    }

    public static WeatherResponse of(WeatherDTO dto, RegionType region) {
        WeatherDTO.Detail weatherDetail = dto.detail().getFirst();
        Detail detail = Detail.builder()
                .temp(weatherDetail.temp())
                .sky(weatherDetail.sky().getCode())
                .pty(weatherDetail.pty().getCode())
                .precipitationAmount(weatherDetail.precipitationAmount())
                .build();
        return WeatherResponse.builder()
                .detail(detail)
                .region(region)
                .date(dto.date())
                .time(weatherDetail.time())
                .build();
    }
}
