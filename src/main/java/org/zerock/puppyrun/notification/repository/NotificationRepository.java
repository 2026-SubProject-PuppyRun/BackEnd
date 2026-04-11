package org.zerock.puppyrun.notification.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import org.zerock.puppyrun.notification.entity.NotificationSettings;

public interface NotificationRepository extends JpaRepository<NotificationSettings, UUID>, NotificationRepoCustom {

    Optional<NotificationSettings> findByMemberId(UUID memberId);
    
    @Transactional
    @Modifying(clearAutomatically = true)
    @Query("UPDATE NotificationSettings n SET n.isActive = false WHERE n.fcmToken IN :tokens")
    void deactivateTokensByFcmToken(@Param("tokens") List<String> tokens);
}
