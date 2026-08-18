package org.zerock.puppyrun.notification.service.sender;

import java.time.LocalTime;
import java.util.UUID;

/** 날씨 추천 메시지 생성에 필요한 회원별 시간과 위치입니다. */
public record WeatherRecommendationTarget(
        UUID memberId,
        Double latitude,
        Double longitude,
        LocalTime time
) {
}
