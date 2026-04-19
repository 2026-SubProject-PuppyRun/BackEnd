package org.zerock.puppyrun.care.controller.response;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.Builder;
import org.zerock.puppyrun.care.entity.VaccinationRecord;
import org.zerock.puppyrun.pet.entity.Pet;

@Builder
public record VaccinationListResponse(
        UUID petId,
        int totalVaccinationCount,
        List<Vaccination> vaccinationList
) {

    public static VaccinationListResponse of(Pet pet, List<VaccinationRecord> vaccinationRecords) {
        List<Vaccination> vaccinations = vaccinationRecords.stream()
                .map(Vaccination::from)
                .toList();

        return VaccinationListResponse.builder()
                .petId(pet.getId())
                .totalVaccinationCount(vaccinations.size())
                .vaccinationList(vaccinations)
                .build();
    }

    @Builder
    public record Vaccination(
            UUID vaccinationId,
            String vaccineName,
            LocalDate vaccinatedAt,
            LocalDate nextVaccinationDate,
            String hospitalName,
            String memo
    ) {

        public static Vaccination from(VaccinationRecord vaccinationRecord) {
            return Vaccination.builder()
                    .vaccinationId(vaccinationRecord.getId())
                    .vaccineName(vaccinationRecord.getVaccineName())
                    .vaccinatedAt(vaccinationRecord.getVaccinatedAt())
                    .nextVaccinationDate(vaccinationRecord.getNextVaccinationDate())
                    .hospitalName(vaccinationRecord.getHospitalName())
                    .memo(vaccinationRecord.getMemo())
                    .build();
        }
    }
}
