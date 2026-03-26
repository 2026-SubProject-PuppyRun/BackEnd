package org.zerock.puppyrun.tracking.DTO;

import java.time.LocalDate;
import lombok.Builder;


@Builder
public record MonthlyTrackingSummary(
        LocalDate date,
        Integer distance,
        Integer duration
) {
}
