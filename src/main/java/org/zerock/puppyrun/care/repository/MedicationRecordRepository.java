package org.zerock.puppyrun.care.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.zerock.puppyrun.care.entity.MedicationRecord;

@Repository
public interface MedicationRecordRepository extends JpaRepository<MedicationRecord, UUID> {

    List<MedicationRecord> findAllByPetIdOrderByAdministeredAtDesc(UUID petId);

    Optional<MedicationRecord> findByIdAndPetId(UUID medicationRecordId, UUID petId);
}
