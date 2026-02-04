package org.zerock.puppyrun.weather;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.zerock.puppyrun.common.scheduler.WeatherScheduler; // [추가] 스케줄러 import
import org.zerock.puppyrun.weather.DTO.RegionType;
import org.zerock.puppyrun.weather.DTO.WeatherDTO;
import org.zerock.puppyrun.weather.service.WeatherService;

@SpringBootTest
class WeatherServiceTest {

    @Autowired
    private WeatherService weatherService;

    @Autowired
    private WeatherScheduler weatherScheduler;

    @Autowired
    private CacheManager cacheManager;

    @BeforeEach
    void before() {

    }

    @Test
    @DisplayName("1. 실제 외부 API 호출 테스트 - 서울 지역 날씨 조회")
    void testRealApiCall() {
        // given
        RegionType region = RegionType.SEOUL;

        // when
        List<WeatherDTO> result = weatherService.getRegionalWeather(region);

        // then
        assertThat(result).isNotEmpty();

        WeatherDTO firstItem = result.getFirst();
        System.out.println("날짜: " + firstItem.date());
        System.out.println("시간: " + firstItem.time());
        System.out.println("온도: " + firstItem.detail().temp());
        System.out.println("하늘: " + firstItem.detail().sky());
        System.out.println("강수: " + firstItem.detail().pty());

        assertThat(firstItem.date()).isNotNull();
        assertThat(firstItem.time()).isNotNull();
        assertThat(firstItem.detail().sky()).isNotNull();
        assertThat(firstItem.detail().pty()).isNotNull();
        assertThat(firstItem.detail().temp()).isNotNull();
    }

    @Test
    @DisplayName("2. 현재 시간(반올림) 기준 날씨 데이터 필터링 테스트")
    void testGetNearestTimeWeather_RealData() {
        // given
        RegionType region = RegionType.SEOUL;
        List<WeatherDTO> weatherList = weatherService.getRegionalWeather(region);

        // when
        WeatherDTO nearestWeather = weatherService.getNearestTimeWeather(weatherList);

        // then
        assertThat(nearestWeather).isNotNull();
        System.out.println("기준 시간: " + nearestWeather.time());
        System.out.println("온도: " + nearestWeather.detail().temp());
    }

    @Test
    @DisplayName("3. 비동기 클라이언트(WebClient) 연동 확인")
    void testAsyncClientIntegration() {
        // given
        RegionType region = RegionType.INCHEON;

        // when
        List<WeatherDTO> result = weatherService.getRegionalWeather(region);

        // then
        assertThat(result).isNotNull();
        assertThat(result.size()).isGreaterThan(0);
    }

    @Test
    @DisplayName("4. 캐싱 동작 확인 - 실제 API 호출 후 캐시 저장 및 조회")
    void testCaching_RealApi() {
        // given
        RegionType region = RegionType.BUSAN;
        String cacheKey = region.name();

        if (cacheManager.getCache("RegionalWeather") != null) {
            cacheManager.getCache("RegionalWeather").evict(cacheKey);
        }

        // when 1
        long startTime1 = System.currentTimeMillis();
        List<WeatherDTO> firstCall = weatherService.getRegionalWeather(region);
        long duration1 = System.currentTimeMillis() - startTime1;

        // then 1
        assertThat(firstCall).isNotEmpty();
        assertThat(cacheManager.getCache("RegionalWeather").get(cacheKey)).isNotNull();
        System.out.println("첫 번째 호출 소요 시간: " + duration1 + "ms");

        // when 2
        long startTime2 = System.currentTimeMillis();
        List<WeatherDTO> secondCall = weatherService.getRegionalWeather(region);
        long duration2 = System.currentTimeMillis() - startTime2;

        // then 2
        assertThat(secondCall).isEqualTo(firstCall);
        System.out.println("두 번째 호출 소요 시간: " + duration2 + "ms");
        assertThat(duration2).isLessThan(duration1);
    }

    @Test
    @DisplayName("5. 스케줄러 동작 확인 - 주기적 업데이트가 캐시에 반영되는지 테스트")
    void testSchedulerExecution() throws InterruptedException {
        // given
        String cacheName = "RegionalWeather";
        // 테스트할 지역 키 (예: SEOUL)
        String key = RegionType.SEOUL.name();

        // 캐시 초기화 (기존 데이터 제거하여 확실한 테스트 환경 조성)
        if (cacheManager.getCache(cacheName) != null) {
            cacheManager.getCache(cacheName).clear();
        }

        // 초기 상태: 캐시가 비어있어야 함
        assertThat(cacheManager.getCache(cacheName).get(key)).isNull();

        // when
        // 스케줄러 메서드 강제 실행 (내부적으로 비동기 API 호출 수행)
        System.out.println("스케줄러 수동 실행 시작...");
        weatherScheduler.scheduledWeatherUpdate();

        // then
        // 비동기 작업(API 호출 -> 캐시 저장)이 완료될 때까지 잠시 대기 (최대 10초)
        boolean isCached = false;
        for (int i = 0; i < 10; i++) {
            if (cacheManager.getCache(cacheName).get(key) != null) {
                isCached = true;
                break;
            }
            Thread.sleep(1000); // 1초 대기
        }

        // 캐시에 데이터가 들어왔는지 확인
        assertThat(isCached).as("스케줄러 실행 후 캐시에 데이터가 저장되어야 합니다.").isTrue();

        // 캐시된 데이터 내용 확인
        @SuppressWarnings("unchecked")
        List<WeatherDTO> cachedData = (List<WeatherDTO>) cacheManager.getCache(cacheName).get(key).get();

        assertThat(cachedData).isNotNull();
        assertThat(cachedData).isNotEmpty();

        System.out.println("캐시된 지역: " + key);
        System.out.println("데이터 개수: " + cachedData.size());
        System.out.println("데이터: " + cachedData);
    }
}
