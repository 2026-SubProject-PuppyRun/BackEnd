package org.zerock.puppyrun.common.config;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CacheType {
    // 초단기·단기예보를 격자별로 캐싱 (최대 4시간, 2종류 × 250개)
    ULTRA_SHORT_WEATHER("UltraShortWeather", 2 * 60 * 60, 250),
    SHORT_TERM_WEATHER("ShortTermWeather", 4 * 60 * 60, 250),
    FAILED_WEATHER("FailedWeather", 24 * 60 * 60, 250);

    private final String cacheName;     // 캐시 이름
    private final int expiredAfterWrite; // 만료 시간 (초 단위)
    private final int maximumSize;      // 최대 저장 개수
}
