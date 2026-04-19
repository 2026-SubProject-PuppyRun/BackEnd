package org.zerock.puppyrun.care.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.zerock.puppyrun.care.entity.AllergyRecord;
import org.zerock.puppyrun.common.exception.ResourceNotFoundException;

@Repository
public interface AllergyRecordRepository extends JpaRepository<AllergyRecord, UUID> {

    default AllergyRecord findByIdAndVerifyPet(UUID allergyId, UUID petId) {
        return findByIdAndPetId(allergyId, petId)
                .orElseThrow(() -> new ResourceNotFoundException("해당 알러지 기록을 찾을 수 없습니다."));
    }

    List<AllergyRecord> findAllByPetIdOrderByCreatedAtDesc(UUID petId);

    Optional<AllergyRecord> findByIdAndPetId(UUID allergyRecordId, UUID petId);
}
