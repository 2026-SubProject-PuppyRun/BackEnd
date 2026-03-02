package org.zerock.puppyrun.diary.repository;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.zerock.puppyrun.diary.entity.Diary;

@Repository
public interface DiaryRepository extends JpaRepository<Diary, UUID> {
    // JPQL을 사용하여 Diary 엔티티의 id만 조회 (SELECT d.id ...)
    @Query("SELECT d.id FROM Diary d WHERE d.tracking.id = :trackingId")
    Optional<UUID> findIdByTrackingId(@Param("trackingId") UUID trackingId);


    boolean existsByTrackingId(UUID trackingId);

    Optional<Diary> findByTrackingId(UUID trackingId);
}
