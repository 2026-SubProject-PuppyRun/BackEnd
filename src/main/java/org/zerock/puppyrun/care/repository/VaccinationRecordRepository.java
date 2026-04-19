package org.zerock.puppyrun.care.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.zerock.puppyrun.care.entity.VaccinationRecord;

@Repository
public interface VaccinationRecordRepository extends JpaRepository<VaccinationRecord, UUID> {

    List<VaccinationRecord> findAllByPetIdOrderByVaccinatedAtDesc(UUID petId);

    Optional<VaccinationRecord> findByIdAndPetId(UUID vaccinationRecordId, UUID petId);
}
