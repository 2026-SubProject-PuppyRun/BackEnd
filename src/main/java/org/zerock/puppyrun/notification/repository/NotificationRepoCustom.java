package org.zerock.puppyrun.notification.repository;


import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.zerock.puppyrun.notification.entity.NotificationType;
import org.zerock.puppyrun.common.pagination.SliceResult;
import org.zerock.puppyrun.notification.repository.DTO.EnabledNotifications;

public interface NotificationRepoCustom {
    SliceResult<EnabledNotifications> findNextMembers(
            LocalDateTime lastCreatedAt,
            UUID lastMemberId,
            Pageable pageable,
            NotificationType type
    );
}
