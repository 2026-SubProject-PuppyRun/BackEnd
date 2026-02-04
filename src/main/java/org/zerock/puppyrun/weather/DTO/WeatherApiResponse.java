package org.zerock.puppyrun.weather.DTO;

import java.util.List;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record WeatherApiResponse(Response response) {

    public record Response(
            Header header,
            Body body
    ) {
    }

    public record Header(
            String resultCode,
            String resultMsg
    ) {
    }

    public record Body(
            Items items
    ) {
    }

    public record Items(
            List<Item> item
    ) {
    }

    public record Item(
            String baseDate,
            String baseTime,
            String category,
            String fcstDate,
            String fcstTime,
            String fcstValue,
            int nx,
            int ny
    ) {
    }
}
