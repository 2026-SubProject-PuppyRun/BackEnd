package org.zerock.puppyrun.tracking.repository;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.zerock.puppyrun.tracking.entity.Tracking;

@Repository
public interface TrackingRepository extends JpaRepository<Tracking, UUID>, TrackingRepoCustom {

    List<Tracking> findAllByMemberId(UUID memberId);

}
