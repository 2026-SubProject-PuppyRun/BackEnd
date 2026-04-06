package org.zerock.puppyrun.notification.repository;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.zerock.puppyrun.notification.entity.NotificationSettings;

public interface NotificationRepository extends JpaRepository<NotificationSettings, UUID>, NotificationRepoCustom {

    Optional<NotificationSettings> findByMemberId(UUID memberId);
}
