package org.zerock.puppyrun.care.service;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.zerock.puppyrun.care.controller.request.RegisterMedicationRequest;
import org.zerock.puppyrun.care.controller.request.UpdateMedicationRequest;
import org.zerock.puppyrun.care.controller.response.MedicationRecordResponse;
import org.zerock.puppyrun.care.entity.MedicationRecord;
import org.zerock.puppyrun.care.repository.MedicationRecordRepository;
import org.zerock.puppyrun.common.auth.security.UserPrincipal;
import org.zerock.puppyrun.pet.entity.Pet;
import org.zerock.puppyrun.pet.repository.PetRepository;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class MedicationCommandService {

    private final PetRepository petRepository;
    private final MedicationRecordRepository medicationRecordRepository;

    public MedicationRecordResponse registerMedication(
            UserPrincipal userPrincipal,
            UUID petId,
            RegisterMedicationRequest request
    ) {
        Pet pet = petRepository.findByIdAndVerifyOwnership(petId, userPrincipal.id());

        MedicationRecord medicationRecord = MedicationRecord.builder()
                .pet(pet)
                .medicationName(request.medicationName())
                .administeredAt(request.administeredAt())
                .doseAmount(request.doseAmount())
                .doseUnit(request.doseUnit())
                .memo(request.memo())
                .build();

        medicationRecordRepository.save(medicationRecord);
        return MedicationRecordResponse.of(medicationRecord);
    }

    public MedicationRecordResponse updateMedication(
            UserPrincipal userPrincipal,
            UUID petId,
            UUID medicationLogId,
            UpdateMedicationRequest request
    ) {
        Pet pet = petRepository.findByIdAndVerifyOwnership(petId, userPrincipal.id());
        MedicationRecord medicationRecord = medicationRecordRepository.findByIdAndVerifyPet(medicationLogId, pet.getId());

        medicationRecord.update(
                request.medicationName(),
                request.administeredAt(),
                request.doseAmount(),
                request.doseUnit(),
                request.memo()
        );

        return MedicationRecordResponse.of(medicationRecord);
    }

    public void deleteMedication(UserPrincipal userPrincipal, UUID petId, UUID medicationLogId) {
        Pet pet = petRepository.findByIdAndVerifyOwnership(petId, userPrincipal.id());
        MedicationRecord medicationRecord = medicationRecordRepository.findByIdAndVerifyPet(medicationLogId, pet.getId());
        medicationRecordRepository.delete(medicationRecord);
    }
}
