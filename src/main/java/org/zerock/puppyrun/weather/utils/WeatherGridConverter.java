package org.zerock.puppyrun.weather.utils;

import org.springframework.stereotype.Component;
import org.zerock.puppyrun.common.exception.InvalidValueException;
import org.zerock.puppyrun.weather.DTO.GridPoint;

@Component
public class WeatherGridConverter {

    // 기상청 단기예보 격자 기준값
    private static final double EARTH_RADIUS = 6371.00877; // km
    private static final double GRID_SIZE = 5.0;           // km

    private static final double STANDARD_LATITUDE_1 = 30.0;
    private static final double STANDARD_LATITUDE_2 = 60.0;

    private static final double ORIGIN_LONGITUDE = 126.0;
    private static final double ORIGIN_LATITUDE = 38.0;

    private static final double ORIGIN_X = 43.0;
    private static final double ORIGIN_Y = 136.0;

    private static final double DEG_TO_RAD = Math.PI / 180.0;


    public GridPoint convert(double latitude, double longitude) {
        validate(latitude, longitude);

        double re = EARTH_RADIUS / GRID_SIZE;

        double slat1 = STANDARD_LATITUDE_1 * DEG_TO_RAD;
        double slat2 = STANDARD_LATITUDE_2 * DEG_TO_RAD;
        double olon = ORIGIN_LONGITUDE * DEG_TO_RAD;
        double olat = ORIGIN_LATITUDE * DEG_TO_RAD;

        double tan = Math.tan(Math.PI * 0.25 + slat1 * 0.5);
        double sn = Math.tan(Math.PI * 0.25 + slat2 * 0.5) / tan;

        sn = Math.log(Math.cos(slat1) / Math.cos(slat2)) / Math.log(sn);

        double sf = tan;
        sf = Math.pow(sf, sn) * Math.cos(slat1) / sn;

        double ro = Math.tan(
                Math.PI * 0.25 + olat * 0.5
        );

        ro = re * sf / Math.pow(ro, sn);

        double ra = Math.tan(
                Math.PI * 0.25 + latitude * DEG_TO_RAD * 0.5
        );

        ra = re * sf / Math.pow(ra, sn);

        double theta = longitude * DEG_TO_RAD - olon;

        if (theta > Math.PI) {
            theta -= 2.0 * Math.PI;
        }

        if (theta < -Math.PI) {
            theta += 2.0 * Math.PI;
        }

        theta *= sn;

        int nx = (int) Math.floor(
                ra * Math.sin(theta) + ORIGIN_X + 0.5
        );

        int ny = (int) Math.floor(
                ro - ra * Math.cos(theta) + ORIGIN_Y + 0.5
        );

        return new GridPoint(nx, ny);
    }

    private void validate(double latitude, double longitude) {
        if (latitude < -90 || latitude > 90) {
            throw new InvalidValueException("위도는 -90 이상 90 이하여야 합니다.");
        }

        if (longitude < -180 || longitude > 180) {
            throw new InvalidValueException("경도는 -180 이상 180 이하여야 합니다.");
        }
    }
}
