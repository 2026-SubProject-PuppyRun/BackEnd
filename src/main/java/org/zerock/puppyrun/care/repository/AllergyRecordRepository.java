package org.zerock.puppyrun.care.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.zerock.puppyrun.care.entity.AllergyRecord;

@Repository
public interface AllergyRecordRepository extends JpaRepository<AllergyRecord, UUID> {

    List<AllergyRecord> findAllByPetIdOrderByCreatedAtDesc(UUID petId);

    Optional<AllergyRecord> findByIdAndPetId(UUID allergyRecordId, UUID petId);
}
