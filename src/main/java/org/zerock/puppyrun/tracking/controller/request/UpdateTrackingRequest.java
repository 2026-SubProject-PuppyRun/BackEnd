package org.zerock.puppyrun.tracking.controller.request;

import java.time.LocalDateTime;
import org.zerock.puppyrun.tracking.entity.Visibility;

public record UpdateTrackingRequest(
        LocalDateTime startedAt,
        LocalDateTime endedAt,
        Integer distance,
        Visibility visibility
) {
}
