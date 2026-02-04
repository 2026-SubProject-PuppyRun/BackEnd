package org.zerock.puppyrun.weather;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.context.bean.override.mockito.MockitoBean; // 변경된 import
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.zerock.puppyrun.common.scheduler.WeatherScheduler;
import org.zerock.puppyrun.weather.DTO.DateTimeDTO;
import org.zerock.puppyrun.weather.DTO.PrecipitationType;
import org.zerock.puppyrun.weather.DTO.RegionType;
import org.zerock.puppyrun.weather.DTO.SkyType;
import org.zerock.puppyrun.weather.DTO.WeatherApiResponse;
import org.zerock.puppyrun.weather.DTO.WeatherDTO;
import org.zerock.puppyrun.weather.service.WeatherApiClient;
import org.zerock.puppyrun.weather.service.WeatherMapper;
import org.zerock.puppyrun.weather.service.WeatherService;
import reactor.core.publisher.Mono;

@SpringBootTest
class WeatherServiceTest {

    @Autowired
    private WeatherService weatherService;

    @Autowired
    private WeatherScheduler weatherScheduler;

    @Autowired
    private CacheManager cacheManager;

    // 외부 API 호출을 흉내 낼 Mock 객체
    @MockitoBean
    private WeatherApiClient weatherApiClient;

    // 응답 변환을 흉내 낼 Mock 객체
    @MockitoBean
    private WeatherMapper weatherMapper;

    private WeatherDTO mockWeatherDTO;

    private String targetTime;
    private String targetDate;

    @BeforeEach
    void setUp() {
        // 테스트 시간 생성
        DateTimeDTO target = weatherService.getTargetTime();
        targetTime = target.baseTime();
        targetDate = target.baseDate();

        // 테스트용 가짜 데이터 생성
        WeatherDTO.Detail detail = new WeatherDTO.Detail("25.0", SkyType.SUNNY, PrecipitationType.NONE);
        mockWeatherDTO = new WeatherDTO(targetDate, targetTime, detail);

        // DateTimeDTO 생성 요청 시 가짜 날짜 반환
        given(weatherApiClient.createCurrentDateTimeDto())
                .willReturn(new DateTimeDTO(targetDate, targetTime));

        // API 호출 시 가짜 JSON(또는 객체) 반환 (Mono)
        WeatherApiResponse mockResponse = Mockito.mock(WeatherApiResponse.class);

        given(weatherApiClient.fetchWeather(any()))
                .willReturn(Mono.just(mockResponse));

        given(weatherMapper.toWeatherDTOList(any()))
                .willReturn(List.of(mockWeatherDTO));
    }

    @Test
    @DisplayName("1. 서비스 로직 테스트 - Mock 데이터를 통한 날씨 조회")
    void testGetRegionalWeather() {
        // given
        RegionType region = RegionType.SEOUL;

        // when
        List<WeatherDTO> result = weatherService.getRegionalWeather(region);

        // then
        assertThat(result).isNotEmpty();
        WeatherDTO firstItem = result.getFirst();

        // Mock 데이터와 일치하는지 검증
        assertThat(firstItem.date()).isEqualTo(targetDate);
        assertThat(firstItem.detail().temp()).isEqualTo("25.0");
        assertThat(firstItem.detail().sky()).isEqualTo(SkyType.SUNNY);

        System.out.println("Mock 날씨 데이터 확인: " + firstItem);
    }

    @Test
    @DisplayName("2. 현재 시간 기준 날씨 데이터 필터링 테스트")
    void testGetNearestTimeWeather() {
        // given
        RegionType region = RegionType.SEOUL;
        List<WeatherDTO> weatherList = weatherService.getRegionalWeather(region);

        // when
        WeatherDTO nearestWeather = weatherService.getNearestTimeWeather(weatherList);

        // then
        assertThat(nearestWeather).isNotNull();
        assertThat(nearestWeather.time()).isEqualTo(targetTime);
    }
//
//    @Test
//    @DisplayName("3. API 클라이언트 호출 여부 확인")
//    void testApiClientCalled() {
//        // given
//        RegionType region = RegionType.INCHEON;
//
//        // when
//        weatherService.getRegionalWeather(region);
//
//        // then
//        // weatherService가 내부적으로 API Client를 호출했는지 검증
//        // (캐시가 비어있을 경우 호출됨)
//        // 주의: @Cacheable 때문에 이전 테스트의 캐시가 남아있을 수 있으므로 캐시 정리 필요할 수 있음
//    }

    @Test
    @DisplayName("4. 캐싱 동작 확인 - API 호출 횟수 검증")
    void testCaching() {
        // given
        RegionType region = RegionType.BUSAN;
        String cacheKey = region.name();
        String cacheName = "RegionalWeather";

        // 캐시 초기화
        if (cacheManager.getCache(cacheName) != null) {
            cacheManager.getCache(cacheName).evict(cacheKey);
        }

        // when 1: 첫 번째 호출 (API 호출 발생 O)
        weatherService.getRegionalWeather(region);

        // then 1
        verify(weatherApiClient, times(1)).fetchWeather(any()); // 호출 1회 확인

        // when 2: 두 번째 호출 (캐시 사용, API 호출 발생 X)
        weatherService.getRegionalWeather(region);

        // then 2
        verify(weatherApiClient, times(1)).fetchWeather(any()); // 여전히 호출 횟수는 1회여야 함
        assertThat(cacheManager.getCache(cacheName).get(cacheKey)).isNotNull(); // 캐시에 데이터 존재 확인

        System.out.println("캐싱 동작 검증 완료: API 호출 1회 유지됨");
    }

    @Test
    @DisplayName("5. 스케줄러 동작 확인 - Mock API를 통한 캐시 갱신")
    void testSchedulerExecution() throws InterruptedException {
        // given
        String cacheName = "RegionalWeather";
        String key = RegionType.SEOUL.name();

        // 캐시 초기화
        if (cacheManager.getCache(cacheName) != null) {
            cacheManager.getCache(cacheName).clear();
        }

        // when
        System.out.println("스케줄러 수동 실행...");
        weatherScheduler.scheduledWeatherUpdate();

        // then
        // 비동기 실행 대기 (Mock이라 빠르지만, delayElements(500ms)가 있으므로 약간 대기)
        boolean isCached = false;
        for (int i = 0; i < 10; i++) {
            if (cacheManager.getCache(cacheName).get(key) != null) {
                isCached = true;
                break;
            }
            Thread.sleep(500);
        }

        assertThat(isCached).as("스케줄러 실행 후 캐시에 데이터가 저장되어야 합니다.").isTrue();

        // API가 지역 개수만큼 호출되었는지 검증 (선택 사항)
        // verify(weatherApiClient, atLeastOnce()).fetchWeather(any());
    }
}
