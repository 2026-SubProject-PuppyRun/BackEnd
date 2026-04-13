package org.zerock.puppyrun.tracking.DTO;

import java.time.LocalDate;
import lombok.Builder;


@Builder
public record DailyTrackingSummary(
        LocalDate date,
        Integer trackingCount,
        Integer distance,
        Integer duration,
        Integer restDuration
) {
}
