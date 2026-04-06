package org.zerock.puppyrun.notification.repository;

import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.zerock.puppyrun.member.entity.Status;
import org.zerock.puppyrun.notification.entity.NotificationType;
import org.zerock.puppyrun.notification.repository.DTO.EnabledNotifications;

import static org.zerock.puppyrun.notification.entity.QNotificationSettings.notificationSettings;
import static org.zerock.puppyrun.member.entity.QMember.member;

@Repository
@RequiredArgsConstructor
public class NotificationRepoCustomImpl implements NotificationRepoCustom {
    private final JPAQueryFactory queryFactory;

    @Override
    public List<EnabledNotifications> findNextMembers(LocalDateTime lastCreatedAt, Pageable pageable,
                                                      NotificationType type) {
        return queryFactory
                .select(Projections.constructor(EnabledNotifications.class, // 추후 설정이 늘어날 수 있어 DTO로 받음
                        notificationSettings.member.id,
                        notificationSettings.fcmToken,
                        member.createdAt
                ))
                .from(notificationSettings)
                .join(notificationSettings.member, member) // JOIN
                .where(
                        member.status.eq(Status.ACTIVE),
                        notificationSettings.isPushAgreed.eq(true),

                        notificationSettings.optOutTypes.contains(type).not(),

                        // 처음 조회할 때는 lastCreatedAt이 null일 수 있으므로 동적 쿼리 처리
                        lastCreatedAt != null ? member.createdAt.gt(lastCreatedAt) : null
                )
                .orderBy(member.createdAt.asc())
                .limit(pageable.getPageSize()) // Pageable에서 지정한 크기 만큼만 조회
                .fetch();
    }
}
