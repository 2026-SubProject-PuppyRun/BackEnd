package org.zerock.puppyrun.weather.DTO;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import org.zerock.puppyrun.weather.DTO.WeatherForecast.UltraSrtFcst;
import org.zerock.puppyrun.weather.DTO.WeatherForecast.VilageFcst;

/**
 * 예보 종류에 따라 요청 시각, 요청 경로, 데이터 분류 코드를 결정하는 전략입니다.
 */
public sealed interface WeatherForecast permits UltraSrtFcst, VilageFcst {

    WeatherFilterCategory getFilterCategory();

    WeatherApiPara getPara();

    String getKey();

    /**
     * 매시 제공되는 초단기예보 요청 전략입니다.
     */
    record UltraSrtFcst(RegionType regionType, LocalDateTime time) implements WeatherForecast {

        @Override
        public WeatherFilterCategory getFilterCategory() {
            return new WeatherFilterCategory("T1H", "SKY", "PTY");
        }

        @Override
        public WeatherApiPara getPara() {
            LocalDateTime baseDateTime = time.minusHours(1);
            return WeatherApiPara.builder()
                    .path("/getUltraSrtFcst")
                    .baseDate(baseDateTime.format(DateTimeFormatter.ofPattern("yyyyMMdd")))
                    .baseTime(baseDateTime.format(DateTimeFormatter.ofPattern("HH00")))
                    .nx(regionType.getNx())
                    .ny(regionType.getNy())
                    .numOfRows(60)
                    .pageNo(1)
                    .build();
        }

        @Override
        public String getKey() {
            return getClass().getName() + "_" + regionType.getName();
        }
    }

    /**
     * 3시간 간격으로 제공되는 단기예보 요청 전략입니다.
     */
    record VilageFcst(RegionType regionType, LocalDateTime time) implements WeatherForecast {

        @Override
        public WeatherFilterCategory getFilterCategory() {
            return new WeatherFilterCategory("TMP", "SKY", "PTY");
        }

        @Override
        public WeatherApiPara getPara() {
            LocalDateTime adjusted = time.minusMinutes(30);
            int hour = adjusted.getHour();
            int baseHour;

            if (hour < 2) {
                baseHour = 23;
                adjusted = adjusted.minusDays(1);
            } else {
                baseHour = ((hour - 2) / 3) * 3 + 2;
            }

            return WeatherApiPara.builder()
                    .path("/getVilageFcst")
                    .baseDate(adjusted.format(DateTimeFormatter.ofPattern("yyyyMMdd")))
                    .baseTime(String.format("%02d00", baseHour))
                    .nx(regionType.getNx())
                    .ny(regionType.getNy())
                    .numOfRows(300)
                    .pageNo(1)
                    .build();
        }

        @Override
        public String getKey() {
            return getClass().getName() + "_" + regionType.getName();
        }
    }
}
