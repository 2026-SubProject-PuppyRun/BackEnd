package org.zerock.puppyrun.weather.service;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.zerock.puppyrun.common.exception.ExternalApiParsingException;
import org.zerock.puppyrun.weather.DTO.PrecipitationType;
import org.zerock.puppyrun.weather.DTO.SkyType;
import org.zerock.puppyrun.weather.DTO.WeatherApiResponse;
import org.zerock.puppyrun.weather.DTO.WeatherApiResponse.Item;
import org.zerock.puppyrun.weather.DTO.WeatherDTO;
import org.zerock.puppyrun.weather.DTO.WeatherFilterCategory;

/**
 * 기상청 예보 API 응답을 애플리케이션에서 사용하는 {@link WeatherDTO}로 변환합니다.
 *
 * <p>API 응답은 하나의 예보 시각마다 기온, 하늘 상태, 강수 상태, 강수량이 각각 별도 항목으로 전달됩니다.
 * 이 클래스는 같은 날짜와 시각의 항목을 하나의 {@link WeatherDTO.WeatherList}로 합치고, 날짜 경계를 포함한 전체 예보를 하나의 {@link WeatherDTO}에 시간순으로
 * 담습니다.</p>
 */
@Component
@Slf4j
public class WeatherMapper {

    // 기상청 초단기예보 응답에서 사용할 기본 분류 코드입니다.
    final String TEMP = "T1H";
    final String SKY = "SKY";
    final String PTY = "PTY";
    final int ULTRA_SHORT_FORECAST_LIMIT = 6;

    /**
     * 초단기예보의 기본 분류 코드로 최대 6개 시간대의 날씨를 변환합니다.
     *
     * @param response 기상청 예보 API 응답
     * @return 예보 날짜와 시간대별 상세 날씨를 담은 DTO
     * @throws ExternalApiParsingException 응답 구조나 예보 데이터가 올바르지 않은 경우
     */
    public WeatherDTO toWeatherDTO(WeatherApiResponse response) {
        return toWeatherDTO(
                response,
                new WeatherFilterCategory(TEMP, SKY, PTY, "RN1"),
                ULTRA_SHORT_FORECAST_LIMIT
        );
    }

    /**
     * 예보 종류별 분류 코드를 사용해 날짜 경계를 포함한 전체 시간대 응답을 변환합니다.
     *
     * @param response 기상청 예보 API 응답
     * @param category 기온, 하늘 상태, 강수 상태, 강수량에 해당하는 분류 코드
     * @return 예보 날짜와 시간대별 상세 날씨를 담은 DTO
     * @throws ExternalApiParsingException 응답 구조나 예보 데이터가 올바르지 않은 경우
     */
    public WeatherDTO toWeatherDTO(
            WeatherApiResponse response,
            WeatherFilterCategory category
    ) {
        return toWeatherDTO(response, category, Long.MAX_VALUE);
    }

    private WeatherDTO toWeatherDTO(
            WeatherApiResponse response,
            WeatherFilterCategory category,
            long limit
    ) {
        // 응답의 필수 중첩 구조가 없으면 정상적인 날씨 데이터로 변환할 수 없습니다.
        if (response == null || response.response() == null ||
                response.response().body() == null ||
                response.response().body().items() == null) {
            throw new ExternalApiParsingException("날씨 API 응답 구조가 올바르지 않습니다.");
        }

        List<WeatherApiResponse.Item> items = response.response().body().items().item();

        if (items == null) {
            throw new ExternalApiParsingException("날씨 데이터 아이템 목록이 비어있습니다.");
        }

        // 같은 예보 날짜와 시각에 속한 분류별 항목을 하나의 상세 날씨로 만들기 위해 그룹화합니다.
        Map<String, List<WeatherApiResponse.Item>> groupedByTime = items.stream()
                .collect(Collectors.groupingBy(item -> item.fcstDate() + item.fcstTime()));

        // 그룹 키는 yyyyMMddHHmm 형식이므로 문자열 정렬만으로 시간순 정렬이 보장됩니다.
        List<Entry<String, List<Item>>> sortedForecasts = groupedByTime.entrySet().stream()
                .sorted(Entry.comparingByKey())
                .toList();

        if (sortedForecasts.isEmpty()) {
            throw new ExternalApiParsingException("날씨 데이터 아이템 목록이 비어있습니다.");
        }

        List<WeatherDTO.WeatherList> forecasts = sortedForecasts.stream()
                .limit(limit)
                .map(entry -> createWeatherDetail(entry.getValue(), category))
                .toList();

        return new WeatherDTO(forecasts);
    }

    /**
     * 같은 날짜와 시각의 분류별 항목을 하나의 상세 날씨로 합칩니다.
     */
    private WeatherDTO.WeatherList createWeatherDetail(
            List<WeatherApiResponse.Item> groupItems,
            WeatherFilterCategory category
    ) {
        if (groupItems == null || groupItems.isEmpty()) {
            throw new ExternalApiParsingException("날씨 데이터 그룹이 유효하지 않습니다.");
        }

        WeatherApiResponse.Item baseItem = groupItems.getFirst();

        if (baseItem == null) {
            throw new ExternalApiParsingException("기준 날씨 아이템이 존재하지 않습니다.");
        }
        
        // 분류 코드를 키로 만들어 필요한 날씨 값을 빠르게 찾습니다.
        Map<String, String> valueMap = groupItems.stream()
                .collect(Collectors.toMap(
                        WeatherApiResponse.Item::category,
                        WeatherApiResponse.Item::fcstValue,
                        (existing, replacement) -> existing
                ));

        return buildWeatherDetail(
                baseItem.fcstDate(),
                baseItem.fcstTime(),
                valueMap,
                category
        );
    }

    /**
     * 분류 코드별 문자열 값을 도메인 Enum으로 변환해 상세 날씨 DTO를 생성합니다.
     */
    private WeatherDTO.WeatherList buildWeatherDetail(
            String forecastDate,
            String forecastTime,
            Map<String, String> valueMap,
            WeatherFilterCategory category
    ) {
        SkyType skyType = SkyType.fromCode(valueMap.getOrDefault(category.sky(), "-1"));
        PrecipitationType ptyType = PrecipitationType.fromCode(valueMap.getOrDefault(category.pty(), "-1"));
        String temp = valueMap.getOrDefault(category.temp(), "-1");
        String precipitationAmount = valueMap.getOrDefault(category.precipitationAmount(), "-");

        return WeatherDTO.WeatherList.builder()
                .date(forecastDate)
                .time(forecastTime)
                .sky(skyType)
                .pty(ptyType)
                .temp(temp)
                .pcp(precipitationAmount)
                .build();
    }
}
