package org.zerock.puppyrun.care.service;

import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.zerock.puppyrun.care.controller.response.VaccinationListResponse;
import org.zerock.puppyrun.care.entity.VaccinationRecord;
import org.zerock.puppyrun.care.repository.VaccinationRecordRepository;
import org.zerock.puppyrun.common.auth.security.UserPrincipal;
import org.zerock.puppyrun.pet.entity.Pet;
import org.zerock.puppyrun.pet.repository.PetRepository;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VaccinationQueryService {

    private final PetRepository petRepository;
    private final VaccinationRecordRepository vaccinationRecordRepository;

    public VaccinationListResponse getVaccinationList(UserPrincipal userPrincipal, UUID petId) {
        Pet pet = petRepository.findByIdAndVerifyOwnership(petId, userPrincipal.id());
        List<VaccinationRecord> vaccinationRecords =
                vaccinationRecordRepository.findAllByPetIdOrderByVaccinatedAtDescCreatedAtDesc(pet.getId());

        return VaccinationListResponse.of(pet, vaccinationRecords);
    }
}
