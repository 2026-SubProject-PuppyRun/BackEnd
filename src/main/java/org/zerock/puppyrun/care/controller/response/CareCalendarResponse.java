package org.zerock.puppyrun.care.controller.response;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.Builder;
import org.zerock.puppyrun.care.entity.CareEventType;
import org.zerock.puppyrun.pet.entity.Pet;

@Builder
public record CareCalendarResponse(
        UUID petId,
        LocalDate startDate,
        LocalDate endDate,
        int totalEventCount,
        List<CareEvent> eventList
) {

    public static CareCalendarResponse of(Pet pet, LocalDate startDate, LocalDate endDate, List<CareEvent> events) {
        return CareCalendarResponse.builder()
                .petId(pet.getId())
                .startDate(startDate)
                .endDate(endDate)
                .totalEventCount(events.size())
                .eventList(events)
                .build();
    }

    @Builder
    public record CareEvent(
            LocalDate date,
            CareEventType eventType,
            String title,
            UUID relatedId,
            String displayText
    ) {
    }
}
