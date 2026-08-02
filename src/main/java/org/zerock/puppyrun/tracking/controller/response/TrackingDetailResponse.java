package org.zerock.puppyrun.tracking.controller.response;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.Builder;
import org.zerock.puppyrun.common.s3.support.S3Url;
import org.zerock.puppyrun.diary.entity.Diary;
import org.zerock.puppyrun.tracking.entity.Tracking;
import org.zerock.puppyrun.tracking.entity.RoutePoint;
import org.zerock.puppyrun.tracking.entity.TrackingImage;
import org.zerock.puppyrun.tracking.util.PaceConverter;

@Builder
public record TrackingDetailResponse(
        UUID id,                     // 산책 고유 아이디
        LocalDateTime startedAt,     // 산책 시작 시간
        LocalDateTime endedAt,       // 산책 종료 시간
        Integer duration,            // 산책 진행 시간
        String visibility,           // 공개 여부
        Integer distance,            // 이동 거리
        List<TrackingImages> trackingImages, // 이미지 리스트
        String averagePace,              // 평균 속도
        List<RoutePoint> path,     // 이동 경로 리스트
        DiaryInfo diaryInfo
) {


    public record TrackingImages(
            Integer order,                     // 순서
            @S3Url String image          // 이미지 리스트

    ) {
        public static TrackingImages of(TrackingImage image) {
            return new TrackingImages(image.getImageOrder(), image.getImageUrl());
        }
    }

    @Builder
    public record DiaryInfo(
            UUID diaryId,
            LocalDateTime writingTime,
            String content,
            Weather weather
    ) {
        public static DiaryInfo of(Diary diary) {
            if (diary == null) {
                return null;
            }

            return DiaryInfo.builder()
                    .diaryId(diary.getId())
                    .content(diary.getContent())
                    .writingTime(diary.getWritingTime())
                    .weather(new Weather(
                            diary.getTemp(),
                            diary.getSky().getCode(),
                            diary.getPty().getCode())
                    )
                    .build();
        }

        public record Weather(
                String temp,
                String skyCode,
                String ptyCode
        ) {
        }
    }

    public static TrackingDetailResponse of(Tracking tracking, List<RoutePoint> path, Diary diary) {
        List<TrackingImages> images = tracking.getTrackingImages().stream()
                .map(TrackingImages::of)
                .toList();

        return TrackingDetailResponse.builder()
                .id(tracking.getId())
                .diaryInfo(DiaryInfo.of(diary))
                .startedAt(tracking.getStartedAt())
                .endedAt(tracking.getEndedAt())
                .duration(tracking.getDuration())
                .visibility(tracking.getVisibility().name())
                .distance(tracking.getDistance())
                .trackingImages(images)
                .averagePace(PaceConverter.toString(tracking.getAveragePace()))
                .path(path)
                .build();
    }


}
