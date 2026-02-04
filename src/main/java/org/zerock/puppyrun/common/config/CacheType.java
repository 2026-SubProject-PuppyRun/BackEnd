package org.zerock.puppyrun.common.config;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CacheType {
    // 지역별 기후를 캐싱 (1시간마다 갱신, 최대 3개)
    REGIONAL_WEATHER("RegionalWeather", 60 * 60, 30);
    private final String cacheName;     // 캐시 이름
    private final int expiredAfterWrite; // 만료 시간 (초 단위)
    private final int maximumSize;      // 최대 저장 개수
}
