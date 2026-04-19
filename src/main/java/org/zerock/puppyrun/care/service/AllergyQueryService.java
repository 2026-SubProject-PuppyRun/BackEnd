package org.zerock.puppyrun.care.service;

import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.zerock.puppyrun.care.controller.response.AllergyListResponse;
import org.zerock.puppyrun.care.entity.AllergyRecord;
import org.zerock.puppyrun.care.repository.AllergyRecordRepository;
import org.zerock.puppyrun.common.auth.security.UserPrincipal;
import org.zerock.puppyrun.pet.entity.Pet;
import org.zerock.puppyrun.pet.repository.PetRepository;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AllergyQueryService {

    private final PetRepository petRepository;
    private final AllergyRecordRepository allergyRecordRepository;

    public AllergyListResponse getAllergyList(UserPrincipal userPrincipal, UUID petId) {
        Pet pet = petRepository.findByIdAndVerifyOwnership(petId, userPrincipal.id());
        List<AllergyRecord> allergyRecords = allergyRecordRepository.findAllByPetIdOrderByCreatedAtDesc(pet.getId());

        return AllergyListResponse.of(pet, allergyRecords);
    }
}
