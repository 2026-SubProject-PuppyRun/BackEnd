package org.zerock.puppyrun.weather.service;

import java.net.URI;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import org.zerock.puppyrun.common.exception.ExternalApiParsingException;
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

    final String DATA_TYPE = "JSON";

    /**
     * 완성된 요청 파라미터로 기상청 원본 응답을 조회합니다.
     */
    public Mono<WeatherApiResponse> fetchWeather(WeatherApiPara para) {
        URI uri = UriComponentsBuilder.fromHttpUrl(FCST_URL)
                .path(para.path())
                .queryParam("serviceKey", API_KEY)
                .queryParam("pageNo", para.pageNo())
                .queryParam("numOfRows", para.numOfRows())
                .queryParam("dataType", DATA_TYPE)
                .queryParam("base_date", para.baseDate())
                .queryParam("base_time", para.baseTime())
                .queryParam("nx", para.nx())
                .queryParam("ny", para.ny())
                .build(true)
                .toUri();

        log.info("기상청 API 요청 URI: {}", uri);

        return webClient.get()
                .uri(uri)
                .retrieve()
                .bodyToMono(WeatherApiResponse.class)
                .doOnNext(this::validateApiResponse);
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
