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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cache.CacheManager;
import org.zerock.puppyrun.a_config.TestContainerConfig;
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

class WeatherServiceTest extends TestContainerConfig {

    @Autowired
    private WeatherService weatherService;

    @Autowired
    private CacheManager cacheManager;

    @MockBean
    private WeatherApiClient weatherApiClient;

    @MockBean
    private WeatherMapper weatherMapper;

    private WeatherDTO mockWeatherDTO;
    private String targetTime;
    private String targetDate;

    @BeforeEach
    void setUp() {
        cacheManager.getCacheNames().forEach(name -> {
            if (cacheManager.getCache(name) != null) {
                cacheManager.getCache(name).clear();
            }
        });

        DateTimeDTO target = weatherService.getTargetTime();
        targetTime = target.baseTime();
        targetDate = target.baseDate();

        WeatherDTO.Detail detail = new WeatherDTO.Detail("25.0", SkyType.SUNNY, PrecipitationType.NONE);
        mockWeatherDTO = new WeatherDTO(targetDate, targetTime, detail);

        given(weatherApiClient.createCurrentDateTimeDto())
                .willReturn(new DateTimeDTO(targetDate, targetTime));

        WeatherApiResponse mockResponse = Mockito.mock(WeatherApiResponse.class);
        given(weatherApiClient.fetchWeather(any()))
                .willReturn(Mono.just(mockResponse));
        given(weatherMapper.toWeatherDTOList(any()))
                .willReturn(List.of(mockWeatherDTO));
    }

    @Test
    @DisplayName("지역 날씨를 조회하고 같은 지역의 재조회에는 캐시를 사용한다")
    void getRegionalWeatherUsesCache() {
        // given
        RegionType region = RegionType.BUSAN;

        // when
        List<WeatherDTO> firstResult = weatherService.getRegionalWeather(region);
        List<WeatherDTO> cachedResult = weatherService.getRegionalWeather(region);

        // then
        assertThat(firstResult).containsExactly(mockWeatherDTO);
        assertThat(cachedResult).isEqualTo(firstResult);
        assertThat(cacheManager.getCache("RegionalWeather").get(region.name())).isNotNull();
        verify(weatherApiClient, times(1)).fetchWeather(any());
        verify(weatherMapper, times(1)).toWeatherDTOList(any());
    }

    @Test
    @DisplayName("조회한 예보 중 현재 시간에 해당하는 날씨를 반환한다")
    void getNearestTimeWeather() {
        // given
        List<WeatherDTO> forecasts = List.of(mockWeatherDTO);

        // when
        WeatherDTO result = weatherService.getNearestTimeWeather(forecasts);

        // then
        assertThat(result).isEqualTo(mockWeatherDTO);
    }
}
