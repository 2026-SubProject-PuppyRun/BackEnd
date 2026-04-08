package org.zerock.puppyrun.notification.repository.DTO;

import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Builder;
import org.zerock.puppyrun.notification.entity.NotificationType;

@Builder
public record EnabledNotifications(
        UUID memberId,
        NotificationType type,
        String fcmToken,
        LocalDateTime createdAt
) {
}
