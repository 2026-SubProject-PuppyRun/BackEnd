package org.zerock.puppyrun.weather.DTO;

import java.util.List;

public record WeatherRegion(
        List<String> names,
        int nx,
        int ny,
        double latitude,
        double longitude
) {
}
