package org.zerock.puppyrun.care.service;

import java.time.LocalDate;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.zerock.puppyrun.care.controller.request.RegisterVaccinationRequest;
import org.zerock.puppyrun.care.controller.request.UpdateVaccinationRequest;
import org.zerock.puppyrun.care.controller.response.VaccinationRecordResponse;
import org.zerock.puppyrun.care.entity.VaccinationRecord;
import org.zerock.puppyrun.care.repository.VaccinationRecordRepository;
import org.zerock.puppyrun.common.auth.security.UserPrincipal;
import org.zerock.puppyrun.common.exception.InvalidValueException;
import org.zerock.puppyrun.pet.entity.Pet;
import org.zerock.puppyrun.pet.repository.PetRepository;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class VaccinationCommandService {

    private final PetRepository petRepository;
    private final VaccinationRecordRepository vaccinationRecordRepository;

    public VaccinationRecordResponse registerVaccination(
            UserPrincipal userPrincipal,
            UUID petId,
            RegisterVaccinationRequest request
    ) {
        Pet pet = petRepository.findByIdAndVerifyOwnership(petId, userPrincipal.id());
        validateDateRange(request.vaccinatedAt(), request.nextVaccinationDate());

        VaccinationRecord vaccinationRecord = VaccinationRecord.builder()
                .pet(pet)
                .vaccineName(request.vaccineName())
                .vaccinatedAt(request.vaccinatedAt())
                .nextVaccinationDate(request.nextVaccinationDate())
                .hospitalName(request.hospitalName())
                .memo(request.memo())
                .build();

        vaccinationRecordRepository.save(vaccinationRecord);
        return VaccinationRecordResponse.of(vaccinationRecord);
    }

    public VaccinationRecordResponse updateVaccination(
            UserPrincipal userPrincipal,
            UUID petId,
            UUID vaccinationId,
            UpdateVaccinationRequest request
    ) {
        Pet pet = petRepository.findByIdAndVerifyOwnership(petId, userPrincipal.id());
        validateDateRange(request.vaccinatedAt(), request.nextVaccinationDate());

        VaccinationRecord vaccinationRecord = vaccinationRecordRepository.findByIdAndVerifyPet(vaccinationId, pet.getId());
        vaccinationRecord.update(
                request.vaccineName(),
                request.vaccinatedAt(),
                request.nextVaccinationDate(),
                request.hospitalName(),
                request.memo()
        );

        return VaccinationRecordResponse.of(vaccinationRecord);
    }

    public void deleteVaccination(UserPrincipal userPrincipal, UUID petId, UUID vaccinationId) {
        Pet pet = petRepository.findByIdAndVerifyOwnership(petId, userPrincipal.id());
        VaccinationRecord vaccinationRecord = vaccinationRecordRepository.findByIdAndVerifyPet(vaccinationId, pet.getId());
        vaccinationRecordRepository.delete(vaccinationRecord);
    }

    private void validateDateRange(LocalDate vaccinatedAt, LocalDate nextVaccinationDate) {
        if (nextVaccinationDate != null && nextVaccinationDate.isBefore(vaccinatedAt)) {
            throw new InvalidValueException("다음 접종 예정일은 접종일보다 이전일 수 없습니다.");
        }
    }
}
