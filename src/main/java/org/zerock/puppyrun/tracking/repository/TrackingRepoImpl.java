package org.zerock.puppyrun.tracking.repository;

import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.zerock.puppyrun.statistics.DTO.MemberTrackingSummary;

import static org.zerock.puppyrun.tracking.entity.QTracking.tracking;

@Repository
@RequiredArgsConstructor
public class TrackingRepoImpl implements TrackingRepoCustom {
    private final JPAQueryFactory queryFactory;


    @Override
    public MemberTrackingSummary getTrackingSummaryByMemberId(UUID memberId) {
        return queryFactory
                .select(Projections.constructor(MemberTrackingSummary.class,
                        tracking.distance.sum().coalesce(0),
                        tracking.duration.sum().coalesce(0),
                        tracking.count()
                ))
                .from(tracking)
                .where(tracking.member.id.eq(memberId))
                .fetchOne();
    }
}
