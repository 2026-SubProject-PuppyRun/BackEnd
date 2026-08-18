package org.zerock.puppyrun.notification.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.zerock.puppyrun.notification.entity.WalkingPreference;

import static org.zerock.puppyrun.notification.entity.QWalkingPreference.walkingPreference;

/**
 * 산책 선호도 스냅샷 Querydsl 구현체입니다.
 */
@Repository
@RequiredArgsConstructor
public class WalkingPreferenceRepoCustomImpl implements WalkingPreferenceRepoCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<WalkingPreference> findByMemberIdsAndCreatedAtBetween(
            Collection<UUID> memberIds,
            LocalDateTime from,
            LocalDateTime to
    ) {
        return queryFactory
                .selectFrom(walkingPreference)
                .where(
                        walkingPreference.member.id.in(memberIds),
                        walkingPreference.createdAt.between(from, to)
                )
                .orderBy(walkingPreference.createdAt.desc(), walkingPreference.id.desc())
                .fetch();
    }
}
