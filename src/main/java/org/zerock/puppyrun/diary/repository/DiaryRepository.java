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

    boolean existsByTrackingId(UUID trackingId);

    Optional<Diary> findByTrackingId(UUID trackingId);
}
