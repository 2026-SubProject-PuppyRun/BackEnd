package org.zerock.puppyrun.tracking.controller.response;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.Builder;
import org.zerock.puppyrun.common.s3.support.S3Url;
import org.zerock.puppyrun.tracking.DTO.TrackingDetailSummary;
import org.zerock.puppyrun.tracking.entity.RoutePoint;
import org.zerock.puppyrun.tracking.util.PaceConverter;

@Builder
public record TrackingDetailResponse(
        UUID id,                     // 산책 고유 아이디
        LocalDateTime startedAt,     // 산책 시작 시간
        LocalDateTime endedAt,       // 산책 종료 시간
        Integer duration,            // 산책 진행 시간
        String visibility,           // 공개 여부
        Integer distance,            // 이동 거리
        @S3Url
        List<TrackingImages> trackingImages, // 이미지 리스트
        String averagePace,              // 평균 속도
        List<RoutePoint> path,     // 이동 경로 리스트
        DiaryInfo diaryInfo,
        List<ParticipatingPet> petList
) {


    public record TrackingImages(
            Integer order,                     // 순서
            @S3Url String image          // 이미지 리스트

    ) {
        private static TrackingImages from(TrackingDetailSummary.TrackingImageSummary image) {
            return new TrackingImages(image.order(), image.image());
        }
    }

    @Builder
    public record DiaryInfo(
            UUID diaryId,
            LocalDateTime writingTime,
            String title,
            String content,
            Weather weather
    ) {
        private static DiaryInfo from(TrackingDetailSummary.DiarySummary diary) {
            if (diary == null) {
                return null;
            }

            return DiaryInfo.builder()
                    .diaryId(diary.diaryId())
                    .content(diary.content())
                    .writingTime(diary.writingTime())
                    .title(diary.title())
                    .weather(new Weather(
                            diary.temp(),
                            diary.skyCode(),
                            diary.ptyCode())
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

    public record ParticipatingPet(
            UUID petId,
            String name,
            @S3Url String profileImage,
            String themeColor
    ) {

        private static ParticipatingPet from(TrackingDetailSummary.ParticipatingPet pet) {
            return new ParticipatingPet(
                    pet.petId(),
                    pet.name(),
                    pet.profileImage(),
                    pet.themeColor()
            );
        }
    }

    public static TrackingDetailResponse from(
            TrackingDetailSummary summary,
            List<TrackingDetailSummary.TrackingImageSummary> imageSummaries,
            List<TrackingDetailSummary.ParticipatingPet> participatingPets
    ) {
        List<TrackingImages> images = imageSummaries.stream()
                .map(TrackingImages::from)
                .toList();

        return TrackingDetailResponse.builder()
                .id(summary.trackingId())
                .diaryInfo(DiaryInfo.from(summary.diary()))
                .startedAt(summary.startedAt())
                .endedAt(summary.endedAt())
                .duration(summary.duration())
                .visibility(summary.visibility().name())
                .distance(summary.distance())
                .trackingImages(images)
                .averagePace(PaceConverter.toString(summary.averagePace()))
                .path(summary.path())
                .petList(participatingPets.stream()
                        .map(ParticipatingPet::from)
                        .toList())
                .build();
    }


}
