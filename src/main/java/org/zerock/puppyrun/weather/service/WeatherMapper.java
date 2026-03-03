package org.zerock.puppyrun.weather.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.zerock.puppyrun.common.exception.ExternalApiParsingException;
import org.zerock.puppyrun.weather.DTO.PrecipitationType;
import org.zerock.puppyrun.weather.DTO.SkyType;
import org.zerock.puppyrun.weather.DTO.WeatherApiResponse;
import org.zerock.puppyrun.weather.DTO.WeatherDTO;

@Component
@Slf4j
public class WeatherMapper {

    final String TEMP = "T1H";
    final String SKY = "SKY";
    final String PTY = "PTY";
    final int FORECAST_LIMIT = 6; // 예보 데이터 제한 개수

    public List<WeatherDTO> toWeatherDTOList(WeatherApiResponse response) {
        // 응답 객체 자체의 Null 체크
        if (response == null || response.response() == null ||
                response.response().body() == null ||
                response.response().body().items() == null) {
            throw new ExternalApiParsingException("날씨 API 응답 구조가 올바르지 않습니다.");
        }

        List<WeatherApiResponse.Item> items = response.response().body().items().item();

        if (items == null) {
            throw new ExternalApiParsingException("날씨 데이터 아이템 목록이 비어있습니다.");
        }

        Map<String, List<WeatherApiResponse.Item>> groupedByTime = items.stream()
                .collect(Collectors.groupingBy(item -> item.fcstDate() + item.fcstTime()));

        return groupedByTime.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .limit(FORECAST_LIMIT)
                .map(entry -> createWeatherDTO(entry.getValue()))
                .collect(Collectors.toList());
    }

    private WeatherDTO createWeatherDTO(List<WeatherApiResponse.Item> groupItems) {
        // 리스트 Null 및 Empty 체크
        if (groupItems == null || groupItems.isEmpty()) {
            throw new ExternalApiParsingException("날씨 데이터 그룹이 유효하지 않습니다.");
        }

        WeatherApiResponse.Item baseItem = groupItems.getFirst();

        // 기준 아이템 Null 체크
        if (baseItem == null) {
            throw new ExternalApiParsingException("기준 날씨 아이템이 존재하지 않습니다.");
        }

        // 로그 출력
        log.info("Weather Processing groupItems : {}", groupItems);

        Map<String, String> valueMap = groupItems.stream()
                .collect(Collectors.toMap(
                        WeatherApiResponse.Item::category,
                        WeatherApiResponse.Item::fcstValue,
                        (existing, replacement) -> existing
                ));

        return WeatherDTO.builder()
                .date(baseItem.fcstDate())
                .time(baseItem.fcstTime())
                .detail(buildWeatherDetail(valueMap))
                .build();
    }

    private WeatherDTO.Detail buildWeatherDetail(Map<String, String> valueMap) {
        SkyType skyType = SkyType.fromCode(valueMap.getOrDefault(SKY, "-1"));
        PrecipitationType ptyType = PrecipitationType.fromCode(valueMap.getOrDefault(PTY, "-1"));
        String temp = valueMap.getOrDefault(TEMP, "-1");

        return WeatherDTO.Detail.builder()
                .sky(skyType)
                .pty(ptyType)
                .temp(temp)
                .build();
    }
}
