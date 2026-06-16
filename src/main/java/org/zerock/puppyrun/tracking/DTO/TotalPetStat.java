package org.zerock.puppyrun.tracking.DTO;

import java.util.UUID;
import lombok.Builder;

@Builder
public record TotalPetStat(
        UUID petId,
        String name,
        String profileImageUrl,
        String themeColor,
        Integer totalDistance,
        Integer totalDuration,
        Long totalCount
) {
}
