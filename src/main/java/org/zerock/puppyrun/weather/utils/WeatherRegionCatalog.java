package org.zerock.puppyrun.weather.utils;

import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Component;
import org.zerock.puppyrun.common.exception.InvalidValueException;
import org.zerock.puppyrun.weather.DTO.WeatherRegion;

@Component
public class WeatherRegionCatalog {
    private final List<WeatherRegion> regions;

    private static final double DEG_TO_RAD = Math.PI / 180.0;

    public WeatherRegionCatalog(List<WeatherRegion> regions) {
        this.regions = List.copyOf(regions);
    }

    public WeatherRegion findNearestRegion(double latitude, double longitude) {
        validate(latitude, longitude);

        return regions.stream()
                .min(Comparator.comparingDouble(region ->
                        calculateHaversineDistance(latitude, longitude, region.latitude(), region.longitude())))
                .orElseThrow(() -> new IllegalStateException("날씨 지역 정보가 존재하지 않습니다."));
    }

    public List<WeatherRegion> getRegions() {
        return List.copyOf(regions);
    }

    /**
     * 유효성 검증
     *
     * @param latitude  경도
     * @param longitude 위도
     */
    private void validate(double latitude, double longitude) {
        if (latitude < -90 || latitude > 90) {
            throw new InvalidValueException("위도는 -90 이상 90 이하여야 합니다.");
        }

        if (longitude < -180 || longitude > 180) {
            throw new InvalidValueException("경도는 -180 이상 180 이하여야 합니다.");
        }
    }

    private double calculateHaversineDistance(
            double latitude,
            double longitude,
            double targetLatitude,
            double targetLongitude
    ) {
        double latitudeDelta = (targetLatitude - latitude) * DEG_TO_RAD;
        double longitudeDelta = (targetLongitude - longitude) * DEG_TO_RAD;
        double latitudeRad = latitude * DEG_TO_RAD;
        double targetLatitudeRad = targetLatitude * DEG_TO_RAD;

        double haversine = Math.pow(Math.sin(latitudeDelta / 2), 2)
                + Math.cos(latitudeRad)
                * Math.cos(targetLatitudeRad)
                * Math.pow(Math.sin(longitudeDelta / 2), 2);
        double normalizedHaversine = Math.max(0, Math.min(1, haversine));

        return 2 * Math.atan2(
                Math.sqrt(normalizedHaversine),
                Math.sqrt(1 - normalizedHaversine)
        );
    }
}
