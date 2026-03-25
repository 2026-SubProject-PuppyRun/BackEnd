package org.zerock.puppyrun.statistics.DTO;

import java.time.LocalDate;
import java.util.List;
import lombok.Builder;

public record WeeklyActivityChart(
        LocalDate startDate,
        LocalDate endDate,
        List<ActivityChart> activityChart
) {
    @Builder
    public record ActivityChart(
            LocalDate date,
            String label,
            Integer distance,
            Integer duration
    ) {
    }
}
