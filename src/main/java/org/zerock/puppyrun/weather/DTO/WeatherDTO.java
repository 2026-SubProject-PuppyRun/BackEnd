package org.zerock.puppyrun.weather.DTO;

import java.util.List;
import lombok.Builder;

@Builder
public record WeatherDTO(
        List<WeatherList> weatherList
) {
    @Builder
    public record WeatherList(
            String date,
            String time,
            String temp, // 온도
            SkyType sky,  // 하늘
            PrecipitationType pty, // 강수 상태
            Double pcp // 강수량
    ) {
        public static WeatherList fromString(
                String date,
                String time,
                String temp,
                String sky,
                String pty,
                String pcp
        ) {
            return new WeatherList(
                    date,
                    time,
                    temp,
                    SkyType.fromCode(sky),
                    PrecipitationType.fromCode(pty),
                    parsePrecipitationMm(pcp)
            );
        }
    }


    private static Double parsePrecipitationMm(String raw) {
        if (raw == null || raw.isBlank()
                || raw.equals("-")
                || raw.equals("강수없음")
                || raw.equals("0")) {
            return 0.0;
        }

        return switch (raw) {
            case "1.0mm 미만" -> 0.1;
            case "30.0~50.0mm" -> 30.0;
            case "50.0mm 이상" -> 50.0;
            default -> Double.parseDouble(raw.replace("mm", ""));
        };

    }

}
