package org.zerock.puppyrun.care.controller.response;

import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Builder;
import org.zerock.puppyrun.care.entity.MedicationRecord;

@Builder
public record MedicationRecordResponse(
        UUID medicationLogId,
        UUID petId,
        String medicationName,
        LocalDateTime administeredAt,
        Double doseAmount,
        String doseUnit,
        String memo
) {

    public static MedicationRecordResponse of(MedicationRecord medicationRecord) {
        return MedicationRecordResponse.builder()
                .medicationLogId(medicationRecord.getId())
                .petId(medicationRecord.getPet().getId())
                .medicationName(medicationRecord.getMedicationName())
                .administeredAt(medicationRecord.getAdministeredAt())
                .doseAmount(medicationRecord.getDoseAmount())
                .doseUnit(medicationRecord.getDoseUnit())
                .memo(medicationRecord.getMemo())
                .build();
    }
}
