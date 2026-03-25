package org.zerock.puppyrun.tracking.controller.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.Builder;

@Builder
public record RegisterTrackingRequest(
        @NotNull(message = "산책 시작 시간은 필수입니다.")
        LocalDateTime startedAt, // 산책 시작 시간

        @NotNull(message = "산책 종료 시간은 필수입니다.")
        LocalDateTime endedAt,   // 산책 종료 시간

        @NotNull(message = "공개 여부는 필수입니다.")
        String visibility,

        @NotNull(message = "이동 거리는 필수입니다.")
        @Min(value = 0, message = "이동 거리는 0 이상이어야 합니다.")
        Integer distance,        // 이동 거리

        @NotEmpty(message = "이동 경로가 비어있습니다.")
        List<TrackingPoint> path, // 이동 경로 리스트

        @NotBlank(message = "평균 속도는 필수입니다.")
        String averagePace,         // 평균 속도

        @NotNull(message = "산책한 펫 ID 리스트는 필수입니다.")
        List<UUID> petIdList      // 산책한 펫 ID 리스트
) {
    @Builder
    public record TrackingPoint(
            @NotNull(message = "위도(lat)는 필수입니다.")
            Double lat,   // 위도

            @NotNull(message = "경도(lng)는 필수입니다.")
            Double lng,   // 경도

            @NotNull(message = "경과 시간은 필수입니다.")
            Integer time  // 경과 시간
    ) {
    }
}
