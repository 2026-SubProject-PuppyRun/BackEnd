package org.zerock.puppyrun.tracking.DTO;

import java.util.UUID;
import org.zerock.puppyrun.tracking.entity.RoutePoint;

public record MainTrackingSummary(
        UUID trackingId,
        String featuredImage,
        java.util.List<RoutePoint> path
) {
}
