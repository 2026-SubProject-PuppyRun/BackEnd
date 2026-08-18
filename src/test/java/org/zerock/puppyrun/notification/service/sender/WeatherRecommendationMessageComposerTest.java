package org.zerock.puppyrun.notification.service.sender;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.zerock.puppyrun.notification.client.DTO.PushTask;
import org.zerock.puppyrun.notification.entity.NotificationType;
import org.zerock.puppyrun.notification.repository.DTO.EnabledNotifications;
import org.zerock.puppyrun.weather.DTO.PrecipitationType;
import org.zerock.puppyrun.weather.DTO.SkyType;
import org.zerock.puppyrun.weather.DTO.WeatherDTO;
import org.zerock.puppyrun.weather.DTO.WeatherRegion;
import org.zerock.puppyrun.weather.service.WeatherQueryService;
import org.zerock.puppyrun.weather.utils.WeatherRegionCatalog;

@ExtendWith(MockitoExtension.class)
class WeatherRecommendationMessageComposerTest {

    @Mock private WeatherRegionCatalog weatherRegionCatalog;
    @Mock private WeatherQueryService weatherQueryService;
    @InjectMocks private WeatherRecommendationMessageComposer composer;

    @Test
    @DisplayName("선호 시간 전후 세 시간 예보를 토큰 메시지 본문으로 만든다")
    void createPushTasks() {
        // given
        UUID memberId = UUID.randomUUID();
        given(weatherRegionCatalog.findNearestRegion(37.5665, 126.9780))
                .willReturn(new WeatherRegion(List.of("서울"), 60, 127, 37.5665, 126.9780));
        given(weatherQueryService.getFcstWeather(any(), any(), eq(3))).willReturn(new WeatherDTO(List.of(
                weather("1700", "25", SkyType.SUNNY, PrecipitationType.NONE, 0.0),
                weather("1800", "24", SkyType.CLOUDY, PrecipitationType.RAIN, 1.2),
                weather("1900", "23", SkyType.OVERCAST, PrecipitationType.NONE, 0.0)
        )));

        // when
        List<PushTask> tasks = composer.createPushTasks(
                List.of(EnabledNotifications.builder().memberId(memberId).fcmToken("token")
                        .type(NotificationType.RECOMMEND_TIME_REMINDER).build()),
                List.of(new WeatherRecommendationTarget(memberId, 37.5665, 126.9780, LocalTime.of(18, 0))),
                LocalDateTime.of(2026, 8, 18, 9, 0)
        );

        // then
        assertThat(tasks).singleElement().satisfies(task -> {
            assertThat(task.body()).contains("17:00 25℃ · 맑음 · 없음");
            assertThat(task.body()).contains("18:00 24℃ · 구름많음 · 비 1.2mm");
        });
        verify(weatherQueryService).getFcstWeather(any(), any(), eq(3));
    }

    private WeatherDTO.WeatherList weather(String time, String temp, SkyType sky, PrecipitationType pty, double pcp) {
        return new WeatherDTO.WeatherList("20260818", time, temp, sky, pty, pcp);
    }
}
