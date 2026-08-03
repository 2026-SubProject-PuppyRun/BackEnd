package org.zerock.puppyrun.weather.controller.response;

import java.util.List;
import lombok.Builder;
import org.zerock.puppyrun.weather.DTO.RegionType;
import org.zerock.puppyrun.weather.DTO.WeatherDTO;

@Builder
public record WeatherForecastResponse(
        RegionType region,
        List<WeatherTime> forecasts
) {

    /**
     * 지역별 예보 레코드를 WeatherForecastResponse로 변환하는 정적 팩토리 메서드
     */
    public static WeatherForecastResponse of(WeatherDTO weather, RegionType region) {
        List<WeatherTime> forecastList = weather.detail().stream()
                .map(detail -> WeatherTime.from(weather.date(), detail))
                .toList();

        return WeatherForecastResponse.builder()
                .region(region)
                .forecasts(forecastList)
                .build();
    }

    @Builder
    public record WeatherTime(
            String date,
            String time,
            Detail detail
    ) {
        /**
         * 단일 시간대의 날씨를 WeatherTime으로 변환
         */
        public static WeatherTime from(String date, WeatherDTO.Detail detail) {
            return WeatherTime.builder()
                    .date(date)
                    .time(detail.time())
                    .detail(Detail.from(detail))
                    .build();
        }
    }

    @Builder
    public record Detail(
            String temp, // 온도
            String sky,  // 하늘 (Code 값)
            String pty,  // 강수 상태 (Code 값)
            String precipitationAmount // 강수량
    ) {
        /**
         * WeatherDTO.Detail을 응답용 Detail로 변환
         */
        public static Detail from(WeatherDTO.Detail dtoDetail) {
            return Detail.builder()
                    .temp(dtoDetail.temp())
                    .sky(dtoDetail.sky().getCode()) // Enum -> Code String 변환
                    .pty(dtoDetail.pty().getCode()) // Enum -> Code String 변환
                    .precipitationAmount(dtoDetail.precipitationAmount())
                    .build();
        }
    }
}
