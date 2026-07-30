package org.zerock.puppyrun.care.controller.response;

import java.time.LocalDate;
import java.util.UUID;
import lombok.Builder;
import org.zerock.puppyrun.care.entity.AllergyRecord;

@Builder
public record AllergyRecordResponse(
        UUID allergyId,
        UUID petId,
        String allergenName,
        String symptom,
        String severity,
        LocalDate identifiedAt,
        Boolean isActive,
        String memo
) {

    public static AllergyRecordResponse of(AllergyRecord allergyRecord) {
        return AllergyRecordResponse.builder()
                .allergyId(allergyRecord.getId())
                .petId(allergyRecord.getPet().getId())
                .allergenName(allergyRecord.getAllergenName())
                .symptom(allergyRecord.getSymptom())
                .severity(allergyRecord.getSeverity() != null ? allergyRecord.getSeverity().name() : null)
                .identifiedAt(allergyRecord.getIdentifiedAt())
                .isActive(allergyRecord.getIsActive())
                .memo(allergyRecord.getMemo())
                .build();
    }
}
