package org.zerock.puppyrun.notification.repository.DTO;

import java.time.LocalDateTime;
import java.util.UUID;

public record EnabledNotifications(
        UUID memberId,
        String fcmToken,
        LocalDateTime createdAt
) {
}
