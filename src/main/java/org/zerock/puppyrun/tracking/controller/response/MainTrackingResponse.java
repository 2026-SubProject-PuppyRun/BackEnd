package org.zerock.puppyrun.tracking.controller.response;

import java.util.List;
import lombok.Builder;
import org.zerock.puppyrun.tracking.entity.Tracking;

@Builder
public record MainTrackingResponse(
        List<TrackingDetailResponse> trackingList
) {
    public static MainTrackingResponse from(List<Tracking> trackingList) {
        List<TrackingDetailResponse> responses = trackingList.stream()
                .map(TrackingDetailResponse::from)
                .toList();

        return MainTrackingResponse.builder()
                .trackingList(responses)
                .build();
    }
}
