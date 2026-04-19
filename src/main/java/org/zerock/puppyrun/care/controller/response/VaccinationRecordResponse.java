package org.zerock.puppyrun.care.controller.response;

import java.time.LocalDate;
import java.util.UUID;
import lombok.Builder;
import org.zerock.puppyrun.care.entity.VaccinationRecord;

@Builder
public record VaccinationRecordResponse(
        UUID vaccinationId,
        UUID petId,
        String vaccineName,
        LocalDate vaccinatedAt,
        LocalDate nextVaccinationDate,
        String hospitalName,
        String memo
) {

    public static VaccinationRecordResponse of(VaccinationRecord vaccinationRecord) {
        return VaccinationRecordResponse.builder()
                .vaccinationId(vaccinationRecord.getId())
                .petId(vaccinationRecord.getPet().getId())
                .vaccineName(vaccinationRecord.getVaccineName())
                .vaccinatedAt(vaccinationRecord.getVaccinatedAt())
                .nextVaccinationDate(vaccinationRecord.getNextVaccinationDate())
                .hospitalName(vaccinationRecord.getHospitalName())
                .memo(vaccinationRecord.getMemo())
                .build();
    }
}
