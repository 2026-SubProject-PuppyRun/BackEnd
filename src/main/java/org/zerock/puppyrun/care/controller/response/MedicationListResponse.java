package org.zerock.puppyrun.care.controller.response;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.Builder;
import org.zerock.puppyrun.care.entity.MedicationRecord;
import org.zerock.puppyrun.pet.entity.Pet;

@Builder
public record MedicationListResponse(
        UUID petId,
        int totalMedicationCount,
        List<MedicationLog> medicationLogList
) {

    public static MedicationListResponse of(Pet pet, List<MedicationRecord> medicationRecords) {
        List<MedicationLog> medicationLogs = medicationRecords.stream()
                .map(MedicationLog::from)
                .toList();

        return MedicationListResponse.builder()
                .petId(pet.getId())
                .totalMedicationCount(medicationLogs.size())
                .medicationLogList(medicationLogs)
                .build();
    }

    @Builder
    public record MedicationLog(
            UUID medicationLogId,
            String medicationName,
            LocalDateTime administeredAt,
            Double doseAmount,
            String doseUnit,
            String memo
    ) {

        public static MedicationLog from(MedicationRecord medicationRecord) {
            return MedicationLog.builder()
                    .medicationLogId(medicationRecord.getId())
                    .medicationName(medicationRecord.getMedicationName())
                    .administeredAt(medicationRecord.getAdministeredAt())
                    .doseAmount(medicationRecord.getDoseAmount())
                    .doseUnit(medicationRecord.getDoseUnit())
                    .memo(medicationRecord.getMemo())
                    .build();
        }
    }
}
