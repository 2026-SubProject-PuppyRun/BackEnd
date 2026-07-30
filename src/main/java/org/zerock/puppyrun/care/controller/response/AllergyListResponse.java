package org.zerock.puppyrun.care.controller.response;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.Builder;
import org.zerock.puppyrun.care.entity.AllergyRecord;
import org.zerock.puppyrun.pet.entity.Pet;

@Builder
public record AllergyListResponse(
        UUID petId,
        int totalAllergyCount,
        List<Allergy> allergyList
) {

    public static AllergyListResponse of(Pet pet, List<AllergyRecord> allergyRecords) {
        List<Allergy> allergies = allergyRecords.stream()
                .map(Allergy::from)
                .toList();

        return AllergyListResponse.builder()
                .petId(pet.getId())
                .totalAllergyCount(allergies.size())
                .allergyList(allergies)
                .build();
    }

    @Builder
    public record Allergy(
            UUID allergyId,
            String allergenName,
            String symptom,
            String severity,
            LocalDate identifiedAt,
            Boolean isActive,
            String memo
    ) {

        public static Allergy from(AllergyRecord allergyRecord) {
            return Allergy.builder()
                    .allergyId(allergyRecord.getId())
                    .allergenName(allergyRecord.getAllergenName())
                    .symptom(allergyRecord.getSymptom())
                    .severity(allergyRecord.getSeverity() != null ? allergyRecord.getSeverity().name() : null)
                    .identifiedAt(allergyRecord.getIdentifiedAt())
                    .isActive(allergyRecord.getIsActive())
                    .memo(allergyRecord.getMemo())
                    .build();
        }
    }
}
