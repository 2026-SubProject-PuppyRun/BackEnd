package org.zerock.puppyrun.weather.DTO;

import java.util.Arrays;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.zerock.puppyrun.common.exception.InvalidValueException;
import org.zerock.puppyrun.weather.exception.WeatherNotFoundException;

@Getter
@RequiredArgsConstructor
public enum PrecipitationType {

    NONE("0", "없음"),
    RAIN("1", "비"),
    RAIN_SNOW("2", "비/눈"),
    SNOW("3", "눈"),
    RAINDROP("5", "빗방울"),
    RAINDROP_SNOW_DRIFT("6", "빗방울날림"),
    SNOW_DRIFT("7", "눈날림");

    private final String code;
    private final String description;

    /**
     * 정수형 코드값을 입력받아 해당하는 Enum 상수를 반환합니다.
     *
     * @param code 강수형태 코드 (0, 1, 2, 3, 5, 6, 7)
     * @return PrecipitationType
     * @throws InvalidValueException 정의되지 않은 코드일 경우 예외 발생
     */
    public static PrecipitationType fromCode(String code) {
        return Arrays.stream(values())
                .filter(type -> type.code.equals(code))
                .findFirst()
                .orElseThrow(() -> new WeatherNotFoundException("잘못된 강수형태 코드입니다: " + code));
    }
}
