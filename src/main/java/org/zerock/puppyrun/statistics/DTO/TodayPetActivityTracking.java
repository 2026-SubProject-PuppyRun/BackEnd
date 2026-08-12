package org.zerock.puppyrun.statistics.DTO;

import java.time.LocalDateTime;
import java.util.UUID;

public record TodayPetActivityTracking(
        UUID petId,
        String petName,
        String PetProfileUrl,

        UUID trackingId,
        Double averagePace,
        LocalDateTime startedAt,
        LocalDateTime endedAt,
        Integer distance,
        Integer duration
) {
}
