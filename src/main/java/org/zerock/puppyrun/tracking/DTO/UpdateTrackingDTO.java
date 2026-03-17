package org.zerock.puppyrun.tracking.DTO;

import java.time.LocalDateTime;
import lombok.Builder;
import org.zerock.puppyrun.tracking.entity.Visibility;

@Builder
public record UpdateTrackingDTO(
        LocalDateTime startedAt,
        LocalDateTime endedAt,
        Visibility visibility
) {
}
