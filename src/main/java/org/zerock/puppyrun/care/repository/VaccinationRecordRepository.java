package org.zerock.puppyrun.care.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.zerock.puppyrun.care.entity.VaccinationRecord;
import org.zerock.puppyrun.common.exception.ResourceNotFoundException;

@Repository
public interface VaccinationRecordRepository extends JpaRepository<VaccinationRecord, UUID> {

    default VaccinationRecord findByIdAndVerifyPet(UUID vaccinationId, UUID petId) {
        return findByIdAndPetId(vaccinationId, petId)
                .orElseThrow(() -> new ResourceNotFoundException("해당 접종 기록을 찾을 수 없습니다."));
    }

    List<VaccinationRecord> findAllByPetIdOrderByVaccinatedAtDescCreatedAtDesc(UUID petId);

    Optional<VaccinationRecord> findByIdAndPetId(UUID vaccinationRecordId, UUID petId);
}
