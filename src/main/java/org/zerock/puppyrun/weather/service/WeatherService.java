package org.zerock.puppyrun.weather.service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.zerock.puppyrun.weather.DTO.DateTimeDTO;
import org.zerock.puppyrun.weather.DTO.RegionType;
import org.zerock.puppyrun.weather.DTO.WeatherApiPara;
import org.zerock.puppyrun.weather.DTO.WeatherApiResponse;
import org.zerock.puppyrun.weather.DTO.WeatherDTO;
import org.zerock.puppyrun.weather.exception.WeatherNotFoundException;

@Service
@RequiredArgsConstructor
@Slf4j
public class WeatherService {

    private final WeatherApiClient weatherApiClient;
    private final WeatherMapper weatherMapper;

    public DateTimeDTO getTargetTime() {
        LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Seoul"));
        LocalDateTime nearestHour = now.plusMinutes(30).truncatedTo(ChronoUnit.HOURS);

        // 비교를 위해 문자열 포맷팅
        String targetDate = nearestHour.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String targetTime = nearestHour.format(DateTimeFormatter.ofPattern("HHmm"));

        return new DateTimeDTO(targetDate, targetTime);
    }

    /**
     * 조회된 예보 리스트 중 현재 시간(30분 단위 반올림)에 가장 적합한 데이터를 필터링
     */
    public WeatherDTO getNearestTimeWeather(List<WeatherDTO> weatherDTOList) {
        DateTimeDTO target = getTargetTime();
        String targetDate = target.baseDate();
        String targetTime = target.baseTime();

        // 리스트에서 해당 시간대의 날씨 찾기
        return weatherDTOList.stream()
                .limit(2)
                .filter(dto -> dto.date().equals(targetDate) && dto.time().equals(targetTime))
                .findFirst()
                .orElseThrow(() -> {
                    log.error("타겟 시간 : {}, 날씨 정보를 찾을 수 없습니다.", targetTime);
                    return new WeatherNotFoundException("해당 시간대의 날씨 정보를 찾을 수 없습니다.");
                });
    }

    /**
     * 지역별 날씨 조회 캐시가 없으면 API Client와 Mapper를 통해 데이터를 가져옵니다.
     */
    @Cacheable(value = "RegionalWeather", key = "#regionType.name()")
    public List<WeatherDTO> getRegionalWeather(RegionType regionType) {
        log.info("캐시 미스 API 직접 호출 진행: {}", regionType.name());

        DateTimeDTO dateTimeDTO = weatherApiClient.createCurrentDateTimeDto();
        WeatherApiPara para = new WeatherApiPara(dateTimeDTO.baseDate(), dateTimeDTO.baseTime(), regionType.getNx(),
                regionType.getNy());

        // 동기 처리 (block)
        WeatherApiResponse response = weatherApiClient.fetchWeather(para).block();

        return weatherMapper.toWeatherDTOList(response);
    }
}
