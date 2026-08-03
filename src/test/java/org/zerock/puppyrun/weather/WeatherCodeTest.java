package org.zerock.puppyrun.weather;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.zerock.puppyrun.common.exception.ErrorCode;
import org.zerock.puppyrun.weather.DTO.PrecipitationType;
import org.zerock.puppyrun.weather.DTO.SkyType;
import org.zerock.puppyrun.weather.exception.InvalidWeatherException;

class WeatherCodeTest {

    @Test
    @DisplayName("정의되지 않은 하늘 상태 코드는 잘못된 날씨 코드 오류로 처리한다")
    void rejectInvalidSkyCode() {
        // given
        String invalidCode = "2";

        // when & then
        assertThatThrownBy(() -> SkyType.fromCode(invalidCode))
                .isInstanceOf(InvalidWeatherException.class)
                .hasMessage("잘못된 하늘상태 코드입니다: 2")
                .satisfies(exception -> assertThat(
                        ((InvalidWeatherException) exception).getErrorCode()
                ).isEqualTo(ErrorCode.INVALID_WEATHER));
    }

    @Test
    @DisplayName("정의되지 않은 강수 형태 코드는 잘못된 날씨 코드 오류로 처리한다")
    void rejectInvalidPrecipitationCode() {
        // given
        String invalidCode = "8";

        // when & then
        assertThatThrownBy(() -> PrecipitationType.fromCode(invalidCode))
                .isInstanceOf(InvalidWeatherException.class)
                .hasMessage("잘못된 강수형태 코드입니다: 8")
                .satisfies(exception -> assertThat(
                        ((InvalidWeatherException) exception).getErrorCode()
                ).isEqualTo(ErrorCode.INVALID_WEATHER));
    }
}
