package org.zerock.puppyrun.tracking.DTO;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.zerock.puppyrun.tracking.entity.RoutePoint;
import org.zerock.puppyrun.tracking.entity.Visibility;

public record TrackingDetailSummary(
        UUID trackingId,
        UUID memberId,
        LocalDateTime startedAt,
        LocalDateTime endedAt,
        Integer duration,
        Visibility visibility,
        Integer distance,
        Double averagePace,
        List<RoutePoint> path,
        DiarySummary diary
) {

    public TrackingDetailSummary {
        path = List.copyOf(path);
    }

    public record TrackingImageSummary(
            Integer order,
            String image
    ) {
    }

    public record DiarySummary(
            UUID diaryId,
            LocalDateTime writingTime,
            String content,
            String temp,
            String skyCode,
            String ptyCode
    ) {
    }

    public record ParticipatingPet(
            UUID petId,
            String name,
            String profileImage,
            String themeColor
    ) {
    }
}
