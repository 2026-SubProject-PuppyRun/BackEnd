package org.zerock.puppyrun.notification.repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.zerock.puppyrun.notification.entity.UserSelectedWalkingTime;

public interface UserSelectedWalkingTimeRepository extends JpaRepository<UserSelectedWalkingTime, UUID> {

    List<UserSelectedWalkingTime> findAllByMemberIdIn(Collection<UUID> memberIds);
}
