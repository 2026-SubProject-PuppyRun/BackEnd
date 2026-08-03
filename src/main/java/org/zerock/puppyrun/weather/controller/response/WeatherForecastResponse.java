package org.zerock.puppyrun.weather.controller.response;

import java.util.List;
import lombok.Builder;
import org.zerock.puppyrun.weather.DTO.WeatherDTO;
import org.zerock.puppyrun.weather.DTO.WeatherRegion;

@Builder
public record WeatherForecastResponse(
        List<String> region,
        List<WeatherTime> forecasts
) {

    /**
     * 지역별 예보 레코드를 WeatherForecastResponse로 변환하는 정적 팩토리 메서드
     */
    public static WeatherForecastResponse of(WeatherDTO weather, WeatherRegion region) {
        List<WeatherTime> forecastList = weather.weatherList().stream()
                .map(WeatherTime::from)
                .toList();

        return WeatherForecastResponse.builder()
                .region(region.names())
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
        public static WeatherTime from(WeatherDTO.WeatherList weather) {
            return WeatherTime.builder()
                    .date(weather.date())
                    .time(weather.time())
                    .detail(Detail.from(weather))
                    .build();
        }
    }

    @Builder
    public record Detail(
            String temp, // 온도
            String sky,  // 하늘 (Code 값)
            String pty,  // 강수 상태 (Code 값)
            String pcp // 강수량
    ) {
        /**
         * WeatherDTO.WeatherList를 응답용 Detail로 변환
         */
        public static Detail from(WeatherDTO.WeatherList dtoDetail) {
            return Detail.builder()
                    .temp(dtoDetail.temp())
                    .sky(dtoDetail.sky().getCode()) // Enum -> Code String 변환
                    .pty(dtoDetail.pty().getCode()) // Enum -> Code String 변환
                    .pcp(dtoDetail.pcp())
                    .build();
        }
    }
}
