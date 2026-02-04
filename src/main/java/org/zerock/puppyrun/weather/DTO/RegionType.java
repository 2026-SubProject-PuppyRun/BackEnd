package org.zerock.puppyrun.weather.DTO;

import java.util.Arrays;
import java.util.Comparator;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum RegionType {
    SEOUL("서울특별시", 60, 127, 37.563569, 126.980008),
    BUSAN("부산광역시", 98, 76, 35.177019, 129.076953),
    DAEGU("대구광역시", 89, 90, 35.868542, 128.603553),
    INCHEON("인천광역시", 55, 124, 37.453233, 126.707353),
    GWANGJU("광주광역시", 58, 74, 35.156975, 126.853364),
    DAEJEON("대전광역시", 67, 100, 36.347119, 127.386567),
    ULSAN("울산광역시", 102, 84, 35.535408, 129.313689),
    SEJONG("세종특별자치시", 66, 103, 36.480012, 127.289069),
    GYEONGGI("경기도", 60, 120, 37.271844, 127.011689),
    GANGWON("강원특별자치도", 73, 134, 37.882692, 127.731975),
    CHUNGBUK("충청북도", 69, 107, 36.632500, 127.493586),
    CHUNGNAM("충청남도", 55, 107, 36.658815, 126.672798),
    JEONBUK("전북특별자치도", 63, 89, 35.817275, 127.111053),
    JEONNAM("전라남도", 51, 67, 34.813044, 126.465000),
    GYEONGBUK("경상북도", 87, 106, 36.575999, 128.505832),
    GYEONGNAM("경상남도", 91, 77, 35.234736, 128.694167),
    JEJU("제주특별자치도", 52, 38, 33.485694, 126.500333);

    private final String name; // 행정구역 명칭
    private final int nx;      // 기상청 격자 X
    private final int ny;      // 기상청 격자 Y
    private final double lat;  // 대표 위도
    private final double lon;  // 대표 경도


    /**
     * 현재 위경도와 가장 가까운 지역을 찾아 반환합니다.
     */
    public static RegionType findNearest(double userLat, double userLon) {
        return Arrays.stream(RegionType.values())
                .min(Comparator.comparingDouble(region ->
                        calculateDistanceSq(userLat, userLon, region.lat, region.lon)))
                .orElse(SEOUL); // 기본값 서울
    }

    /**
     * 두 지점 사이의 거리 제곱을 계산
     */
    private static double calculateDistanceSq(double lat1, double lon1, double lat2, double lon2) {
        double dLat = lat1 - lat2;
        double dLon = lon1 - lon2;
        return (dLat * dLat) + (dLon * dLon);
    }

}
