package org.zerock.puppyrun.tracking.DTO;

import java.util.List;
import java.util.UUID;
import org.zerock.puppyrun.tracking.entity.RoutePoint;

public record MainTrackingSummary(
        UUID trackingId,
        String featuredImage,
        List<RoutePoint> path
) {

    public MainTrackingSummary {
        path = List.copyOf(path);
    }
}
