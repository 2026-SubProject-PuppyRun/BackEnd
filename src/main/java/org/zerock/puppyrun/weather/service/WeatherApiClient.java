package org.zerock.puppyrun.weather.service;

import java.net.URI;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.zerock.puppyrun.common.exception.ExternalApiParsingException;
import org.zerock.puppyrun.weather.DTO.DateTimeDTO;
import org.zerock.puppyrun.weather.DTO.WeatherApiPara;
import org.zerock.puppyrun.weather.DTO.WeatherApiResponse;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
@Slf4j
public class WeatherApiClient {

    private final WebClient webClient;

    @Value("${data-kr.api-key}")
    private String API_KEY;
    @Value("${data-kr.forecest-url}")
    private String FCST_URL;

    final String FCST_URI = "/getUltraSrtFcst";
    final int PAGE_NO = 1;
    final int NUM_OF_ROWS = 100;
    final String DATA_TYPE = "JSON";
    final String TIME_ZONE = "Asia/Seoul";

    /**
     * WebClient를 활용하여 리액티브 타입(Mono)으로 응답을 반환하도록 구현
     */
    public Mono<WeatherApiResponse> fetchWeather(WeatherApiPara para) {
        String uriString = String.format(
                "%s%s?serviceKey=%s&pageNo=%d&numOfRows=%d&dataType=%s&base_date=%s&base_time=%s&nx=%d&ny=%d",
                FCST_URL, FCST_URI, API_KEY, PAGE_NO, NUM_OF_ROWS, DATA_TYPE,
                para.baseDate(), para.baseTime(), para.nx(), para.ny());

        URI uri = URI.create(uriString);

        log.info("Generated URI: {}", uri); // 생성된 URI 확인

        return webClient.get()
                .uri(uri)
                .retrieve()
                .bodyToMono(WeatherApiResponse.class)
                .doOnNext(this::validateApiResponse);
    }

    public DateTimeDTO createCurrentDateTimeDto() {
        LocalDateTime now = LocalDateTime.now(ZoneId.of(TIME_ZONE));
        LocalDateTime baseDateTime = now.minusHours(1);
        String baseDate = baseDateTime.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String baseTime = baseDateTime.format(DateTimeFormatter.ofPattern("HH00"));
        return new DateTimeDTO(baseDate, baseTime);
    }

    private void validateApiResponse(WeatherApiResponse response) {
        if (response == null) {
            throw new ExternalApiParsingException("날씨 API 응답이 비어있습니다.");
        }

        String resultCode = Optional.ofNullable(response.response())
                .map(WeatherApiResponse.Response::header)
                .map(WeatherApiResponse.Header::resultCode)
                .orElse("UNKNOWN");

        if (!"00".equals(resultCode)) {
            String resultMsg = Optional.ofNullable(response.response())
                    .map(WeatherApiResponse.Response::header)
                    .map(WeatherApiResponse.Header::resultMsg)
                    .orElse("메시지 없음");
            log.error("날씨 API 호출 실패 - Code: {}, Msg: {}", resultCode, resultMsg);
            throw new ExternalApiParsingException("날씨 API 호출 실패: %s".formatted(resultMsg));
        }

        Optional.of(response.response())
                .map(WeatherApiResponse.Response::body)
                .map(WeatherApiResponse.Body::items)
                .map(WeatherApiResponse.Items::item)
                .orElseThrow(() -> new ExternalApiParsingException("날씨 데이터(Body/Items)가 비어있습니다."));
    }
}
