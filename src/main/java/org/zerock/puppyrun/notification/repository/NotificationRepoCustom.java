package org.zerock.puppyrun.notification.repository;


import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.zerock.puppyrun.notification.entity.NotificationType;
import org.zerock.puppyrun.notification.repository.DTO.EnabledNotifications;

public interface NotificationRepoCustom {
    List<EnabledNotifications> findNextMembers(LocalDateTime lastCreatedAt, Pageable pageable, NotificationType type);
}
