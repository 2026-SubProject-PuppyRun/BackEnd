package org.zerock.puppyrun.notification.repository;

import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
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
    public List<EnabledNotifications> findNextMembers(
            LocalDateTime lastCreatedAt,
            UUID lastMemberId,
            Pageable pageable,
            NotificationType type
    ) {

        return queryFactory
                .select(Projections.constructor(EnabledNotifications.class,
                        notificationSettings.member.id,
                        Expressions.constant(type),
                        notificationSettings.fcmToken,
                        member.createdAt
                ))
                .from(notificationSettings)
                .join(notificationSettings.member, member)
                .where(
                        member.status.eq(Status.ACTIVE),
                        notificationSettings.isPushAgreed.eq(true),
                        notificationSettings.isActive.eq(true),
                        notificationSettings.optOutTypes.contains(type).not(),
                        afterCursor(lastCreatedAt, lastMemberId)
                )
                .orderBy(member.createdAt.asc(), member.id.asc())
                .limit(pageable.getPageSize() + 1)
                .fetch();

    }

    private BooleanExpression afterCursor(LocalDateTime lastCreatedAt, UUID lastMemberId) {
        if (lastCreatedAt == null || lastMemberId == null) {
            return null;
        }

        return member.createdAt.gt(lastCreatedAt)
                .or(member.createdAt.eq(lastCreatedAt)
                        .and(member.id.gt(lastMemberId)));
    }
}
