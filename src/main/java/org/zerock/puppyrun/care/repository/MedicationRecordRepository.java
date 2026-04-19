package org.zerock.puppyrun.care.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.zerock.puppyrun.care.entity.MedicationRecord;
import org.zerock.puppyrun.common.exception.ResourceNotFoundException;

@Repository
public interface MedicationRecordRepository extends JpaRepository<MedicationRecord, UUID> {

    default MedicationRecord findByIdAndVerifyPet(UUID medicationLogId, UUID petId) {
        return findByIdAndPetId(medicationLogId, petId)
                .orElseThrow(() -> new ResourceNotFoundException("해당 투약 기록을 찾을 수 없습니다."));
    }

    List<MedicationRecord> findAllByPetIdOrderByAdministeredAtDescCreatedAtDesc(UUID petId);

    Optional<MedicationRecord> findByIdAndPetId(UUID medicationRecordId, UUID petId);
}
