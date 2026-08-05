package org.zerock.puppyrun.weather.utils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import org.zerock.puppyrun.common.config.CacheType;
import org.zerock.puppyrun.weather.DTO.GridPoint;
import org.zerock.puppyrun.weather.DTO.WeatherApiPara;
import org.zerock.puppyrun.weather.DTO.WeatherFilterCategory;

/**
 * 예보별 기상청 요청 규칙을 정의하는 확장 지점입니다. 새로운 예보는 이 인터페이스를 구현하면 공통 수집 파이프라인을 그대로 사용할 수 있습니다.
 */
public interface WeatherForecast {

    DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HHmm");

    String getPath();

    int getNumberOfRows();

    LocalDateTime getBaseDateTime();

    WeatherFilterCategory getFilterCategory();

    CacheType getCacheType();

    ForecastType getType();

    LocalDateTime requestTime();

    default boolean isDbBackupRequired() {
        return false;
    }

    default WeatherApiPara getPara(GridPoint gridPoint) {
        LocalDateTime baseDateTime = getBaseDateTime();

        return WeatherApiPara.builder()
                .path(getPath())
                .baseDate(baseDateTime.format(DATE_FORMATTER))
                .baseTime(baseDateTime.format(TIME_FORMATTER))
                .nx(gridPoint.nx())
                .ny(gridPoint.ny())
                .numOfRows(getNumberOfRows())
                .pageNo(1)
                .build();
    }

    /**
     * 매시간 30분에 발표되는 초단기예보 전략입니다.
     */
    record UltraShort(LocalDateTime requestTime) implements WeatherForecast {

        @Override
        public String getPath() {
            return "/getUltraSrtFcst";
        }

        @Override
        public int getNumberOfRows() {
            return 60;
        }

        @Override
        public WeatherFilterCategory getFilterCategory() {
            return new WeatherFilterCategory("T1H", "SKY", "PTY", "RN1");
        }

        @Override
        public CacheType getCacheType() {
            return CacheType.ULTRA_SHORT_WEATHER;
        }

        @Override
        public ForecastType getType() {
            return ForecastType.ULTRA_SHORT;
        }

        @Override
        public LocalDateTime getBaseDateTime() {
            LocalDateTime baseDateTime = this.requestTime
                    .withMinute(30)
                    .withSecond(0)
                    .withNano(0);

            return requestTime.getMinute() < 45
                    ? baseDateTime.minusHours(1)
                    : baseDateTime;
        }
    }

    /**
     * 02시부터 3시간 간격으로 발표되는 단기예보 전략입니다.
     */
    record ShortTerm(LocalDateTime requestTime) implements WeatherForecast {

        @Override
        public String getPath() {
            return "/getVilageFcst";
        }

        @Override
        public int getNumberOfRows() {
            return 300;
        }

        @Override
        public WeatherFilterCategory getFilterCategory() {
            return new WeatherFilterCategory("TMP", "SKY", "PTY", "PCP");
        }

        @Override
        public CacheType getCacheType() {
            return CacheType.SHORT_TERM_WEATHER;
        }

        @Override
        public ForecastType getType() {
            return ForecastType.SHORT_TERM;
        }

        @Override
        public boolean isDbBackupRequired() {
            return true;
        }

        @Override
        public LocalDateTime getBaseDateTime() {
            LocalDateTime adjusted = this.requestTime.minusMinutes(30);
            int hour = adjusted.getHour();

            if (hour < 2) {
                return adjusted
                        .minusDays(1)
                        .withHour(23)
                        .withMinute(0)
                        .withSecond(0)
                        .withNano(0);
            }

            int baseHour = ((hour - 2) / 3) * 3 + 2;
            return adjusted
                    .withHour(baseHour)
                    .withMinute(0)
                    .withSecond(0)
                    .withNano(0);
        }
    }

    enum ForecastType {
        ULTRA_SHORT,
        SHORT_TERM;
    }
}
