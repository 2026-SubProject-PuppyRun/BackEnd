package org.zerock.puppyrun.care.service;

import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.zerock.puppyrun.care.controller.response.MedicationListResponse;
import org.zerock.puppyrun.care.entity.MedicationRecord;
import org.zerock.puppyrun.care.repository.MedicationRecordRepository;
import org.zerock.puppyrun.common.auth.security.UserPrincipal;
import org.zerock.puppyrun.pet.entity.Pet;
import org.zerock.puppyrun.pet.repository.PetRepository;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MedicationQueryService {

    private final PetRepository petRepository;
    private final MedicationRecordRepository medicationRecordRepository;

    public MedicationListResponse getMedicationList(UserPrincipal userPrincipal, UUID petId) {
        Pet pet = petRepository.findByIdAndVerifyOwnership(petId, userPrincipal.id());
        List<MedicationRecord> medicationRecords =
                medicationRecordRepository.findAllByPetIdOrderByAdministeredAtDescCreatedAtDesc(pet.getId());

        return MedicationListResponse.of(pet, medicationRecords);
    }
}
