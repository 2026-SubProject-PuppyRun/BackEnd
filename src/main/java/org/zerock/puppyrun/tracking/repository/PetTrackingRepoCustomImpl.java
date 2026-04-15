package org.zerock.puppyrun.tracking.repository;

import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.zerock.puppyrun.tracking.DTO.TotalPetTracking;

import static org.zerock.puppyrun.pet.entity.QPet.pet;
import static org.zerock.puppyrun.tracking.entity.QPetTracking.petTracking;
import static org.zerock.puppyrun.tracking.entity.QTracking.tracking;

@Repository
@RequiredArgsConstructor
public class PetTrackingRepoCustomImpl implements PetTrackingRepoCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public int sumTotalDistanceByPetId(UUID petId) {
        Integer result = queryFactory
                .select(tracking.distance.sum().coalesce(0))
                .from(petTracking)
                .join(petTracking.tracking, tracking)
                .where(petTracking.pet.id.eq(petId))
                .fetchOne();

        return result != null ? result : 0;
    }

    @Override
    public int sumTotalDurationByPetId(UUID petId) {
        Integer result = queryFactory
                .select(tracking.duration.sum().coalesce(0))
                .from(petTracking)
                .join(petTracking.tracking, tracking)
                .where(petTracking.pet.id.eq(petId))
                .fetchOne();

        return result != null ? result : 0;
    }

    @Override
    public List<TotalPetTracking> getTrackingSummaryByPetId(List<UUID> petId, LocalDate startDate, LocalDate endDate) {
        return queryFactory
                .select(Projections.constructor(TotalPetTracking.class,
                        pet.id,
                        Expressions.constant(startDate),
                        Expressions.constant(endDate),
                        pet.name,
                        pet.profileImageUrl,
                        pet.color,
                        pet.badge,
                        tracking.distance.sum().coalesce(0),
                        tracking.duration.sum().coalesce(0),
                        tracking.count(),
                        tracking.averagePace.avg().coalesce(0.0),
                        tracking.restDuration.sum().coalesce(0)
                ))
                .from(pet)
                .leftJoin(petTracking).on(petTracking.pet.eq(pet))
                .leftJoin(petTracking.tracking, tracking)
                // LocalDate를 LocalDateTime 구간으로 변환하여 검색 (startDate 00:00:00 ~ endDate 다음날 00:00:00 미만)
                .on(
                        tracking.startedAt.goe(startDate.atStartOfDay()) // >= startDate 00:00
                                .and(tracking.startedAt.lt(endDate.plusDays(1).atStartOfDay())) // < endDate+1 00:00
                )
                .where(pet.id.in(petId))
                .groupBy(pet.id, pet.name, pet.profileImageUrl, pet.color, pet.badge)
                .fetch();
    }


    @Override
    public int countTogetherTracking(UUID memberId, LocalDate startDate, LocalDate endDate) {
        // QueryDSL로 산책 ID(Tracking ID)별로 그룹화한 뒤, 참여한 펫이 2마리 이상(> 1)인 산책 ID만 조회합니다.
        List<UUID> togetherTrackingIds = queryFactory
                .select(tracking.id)
                .from(petTracking)
                .join(petTracking.tracking, tracking)
                .where(
                        tracking.member.id.eq(memberId), // 요청한 멤버의 산책 기록 중
                        tracking.startedAt.goe(startDate.atStartOfDay()), // 기간 필터링
                        tracking.startedAt.lt(endDate.plusDays(1).atStartOfDay())
                )
                .groupBy(tracking.id) // 산책 1건 기준으로 묶음
                .having(petTracking.id.count().gt(1L)) // 산책에 참여한 펫이 2마리 이상인 것만 필터 (교집합)
                .fetch();

        // 다견 동반 산책을 한 횟수를 반환
        return togetherTrackingIds.size();
    }

}
