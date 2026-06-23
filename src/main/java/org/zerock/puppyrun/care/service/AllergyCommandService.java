package org.zerock.puppyrun.care.service;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.zerock.puppyrun.care.controller.request.RegisterAllergyRequest;
import org.zerock.puppyrun.care.controller.request.UpdateAllergyRequest;
import org.zerock.puppyrun.care.controller.response.AllergyRecordResponse;
import org.zerock.puppyrun.care.entity.AllergyRecord;
import org.zerock.puppyrun.care.entity.AllergySeverity;
import org.zerock.puppyrun.care.repository.AllergyRecordRepository;
import org.zerock.puppyrun.common.auth.security.UserPrincipal;
import org.zerock.puppyrun.pet.entity.Pet;
import org.zerock.puppyrun.pet.repository.PetRepository;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class AllergyCommandService {

    private final PetRepository petRepository;
    private final AllergyRecordRepository allergyRecordRepository;

    public AllergyRecordResponse registerAllergy(
            UserPrincipal userPrincipal,
            UUID petId,
            RegisterAllergyRequest request
    ) {
        Pet pet = petRepository.findByIdAndVerifyOwnership(petId, userPrincipal.id());
        AllergySeverity severity = toSeverity(request.severity());

        AllergyRecord allergyRecord = AllergyRecord.builder()
                .pet(pet)
                .allergenName(request.allergenName())
                .symptom(request.symptom())
                .severity(severity)
                .identifiedAt(request.identifiedAt())
                .isActive(request.isActive())
                .memo(request.memo())
                .build();

        allergyRecordRepository.save(allergyRecord);
        return AllergyRecordResponse.of(allergyRecord);
    }

    public AllergyRecordResponse updateAllergy(
            UserPrincipal userPrincipal,
            UUID petId,
            UUID allergyId,
            UpdateAllergyRequest request
    ) {
        Pet pet = petRepository.findByIdAndVerifyOwnership(petId, userPrincipal.id());
        AllergyRecord allergyRecord = allergyRecordRepository.findByIdAndVerifyPet(allergyId, pet.getId());
        AllergySeverity severity = toSeverity(request.severity());

        allergyRecord.update(
                request.allergenName(),
                request.symptom(),
                severity,
                request.identifiedAt(),
                request.isActive(),
                request.memo()
        );

        return AllergyRecordResponse.of(allergyRecord);
    }

    public void deleteAllergy(UserPrincipal userPrincipal, UUID petId, UUID allergyId) {
        Pet pet = petRepository.findByIdAndVerifyOwnership(petId, userPrincipal.id());
        AllergyRecord allergyRecord = allergyRecordRepository.findByIdAndVerifyPet(allergyId, pet.getId());
        allergyRecordRepository.delete(allergyRecord);
    }

    private AllergySeverity toSeverity(String severity) {
        if (severity == null) {
            return null;
        }
        return AllergySeverity.from(severity);
    }
}
