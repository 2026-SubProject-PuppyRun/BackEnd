package org.zerock.puppyrun.weather.DTO;

import java.util.Arrays;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.zerock.puppyrun.common.exception.InvalidValueException;
import org.zerock.puppyrun.weather.exception.WeatherNotFoundException;

@Getter
@RequiredArgsConstructor
public enum SkyType {

    SUNNY("1", "맑음"),
    CLOUDY("3", "구름많음"),
    OVERCAST("4", "흐림");

    private final String code;
    private final String description;

    /**
     * 정수형 코드값을 입력받아 해당하는 Enum 상수를 반환합니다.
     *
     * @param code 하늘상태 코드 (1, 3, 4)
     * @return SkyType
     * @throws InvalidValueException 정의되지 않은 코드일 경우 예외 발생
     */
    public static SkyType fromCode(String code) {
        return Arrays.stream(values())
                .filter(type -> type.code.equals(code))
                .findFirst()
                .orElseThrow(() -> new WeatherNotFoundException("잘못된 하늘상태 코드입니다: " + code));
    }
}
