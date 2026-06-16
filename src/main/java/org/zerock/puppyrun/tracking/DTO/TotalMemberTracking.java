package org.zerock.puppyrun.tracking.DTO;

import java.util.UUID;
import lombok.Builder;

@Builder
public record TotalMemberTracking(
        UUID memberId,
        Integer totalDistance,
        Integer totalDuration,
        Long totalCount
) {
}
