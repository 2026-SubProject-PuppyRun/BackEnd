package org.zerock.puppyrun.pet.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.zerock.puppyrun.pet.entity.PetWeightLog;

@Repository
public interface PetWeightLogRepository extends JpaRepository<PetWeightLog, UUID> {

    // 전체 조회
    List<PetWeightLog> findAllByPetIdOrderByCreatedAt(UUID petId);

    List<PetWeightLog> findAllByPetIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThanOrderByCreatedAtAsc(
            UUID petId,
            LocalDateTime startDateTime,
            LocalDateTime endExclusive
    );

    // 최신 데이터 1건만 조회
    Optional<PetWeightLog> findFirstByPetIdOrderByCreatedAtDesc(UUID petId);

}
