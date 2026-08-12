package org.zerock.puppyrun.tracking.repository;

import com.querydsl.core.Tuple;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.DateTemplate;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.zerock.puppyrun.statistics.DTO.PetActivityTracking;
import org.zerock.puppyrun.tracking.DTO.DailyMemberStat;
import org.zerock.puppyrun.tracking.DTO.DailyTracking;
import org.zerock.puppyrun.tracking.DTO.DailyTrackingSummary;
import org.zerock.puppyrun.tracking.DTO.MainTrackingSummary;
import org.zerock.puppyrun.tracking.DTO.TrackingDetailSummary;
import org.zerock.puppyrun.tracking.entity.Tracking;
import org.zerock.puppyrun.tracking.entity.TrackingRoute;


import static org.zerock.puppyrun.diary.entity.QDiary.diary;
import static org.zerock.puppyrun.member.entity.QMember.member;
import static org.zerock.puppyrun.pet.entity.QPet.pet;
import static org.zerock.puppyrun.tracking.entity.QPetTracking.petTracking;
import static org.zerock.puppyrun.tracking.entity.QTracking.tracking;
import static org.zerock.puppyrun.tracking.entity.QTrackingImage.trackingImage;
import static org.zerock.puppyrun.tracking.entity.QTrackingRoute.trackingRoute;

@Repository
@RequiredArgsConstructor
public class TrackingRepoCustomImpl implements TrackingRepoCustom {
    private final JPAQueryFactory queryFactory;

    @Override
    public List<MainTrackingSummary> findMainTrackingSummaries(UUID memberId) {
        return queryFactory
                .select(
                        tracking.id,
                        trackingImage.imageUrl,
                        trackingRoute
                )
                .from(tracking)
                .leftJoin(trackingImage).on(
                        trackingImage.tracking.eq(tracking),
                        trackingImage.imageOrder.eq(0)
                )
                .leftJoin(trackingRoute).on(trackingRoute.tracking.eq(tracking))
                .where(tracking.member.id.eq(memberId))
                .orderBy(
                        tracking.startedAt.desc(),
                        tracking.id.asc()
                )
                .fetch()
                .stream()
                .map(row -> {
                    TrackingRoute route = row.get(trackingRoute);
                    return new MainTrackingSummary(
                            row.get(tracking.id),
                            row.get(trackingImage.imageUrl),
                            route != null ? route.getOriginalPath() : List.of()
                    );
                })
                .toList();
    }

    @Override
    public Optional<TrackingDetailSummary> findTrackingDetailSummary(UUID trackingId) {
        Tuple detailRow = queryFactory
                .select(
                        tracking.id,
                        tracking.member.id,
                        tracking.startedAt,
                        tracking.endedAt,
                        tracking.duration,
                        tracking.visibility,
                        tracking.distance,
                        tracking.averagePace,
                        trackingRoute,
                        diary.id,
                        diary.writingTime,
                        diary.title,
                        diary.content,
                        diary.temp,
                        diary.sky,
                        diary.pty
                )
                .from(tracking)
                .leftJoin(trackingRoute).on(trackingRoute.tracking.eq(tracking))
                .leftJoin(diary).on(diary.tracking.eq(tracking))
                .where(tracking.id.eq(trackingId))
                .fetchOne();

        if (detailRow == null) {
            return Optional.empty();
        }

        return Optional.of(toTrackingDetailSummary(detailRow));
    }

    @Override
    public List<TrackingDetailSummary.TrackingImageSummary> findTrackingImageSummaries(UUID trackingId) {
        return queryFactory
                .select(Projections.constructor(
                        TrackingDetailSummary.TrackingImageSummary.class,
                        trackingImage.imageOrder,
                        trackingImage.imageUrl
                ))
                .from(trackingImage)
                .where(trackingImage.tracking.id.eq(trackingId))
                .orderBy(trackingImage.imageOrder.asc())
                .fetch();
    }

    @Override
    public List<TrackingDetailSummary.ParticipatingPet> findParticipatingPetSummaries(UUID trackingId) {
        return queryFactory
                .select(Projections.constructor(
                        TrackingDetailSummary.ParticipatingPet.class,
                        pet.id,
                        pet.name,
                        pet.profileImageUrl,
                        pet.color
                ))
                .distinct()
                .from(petTracking)
                .join(petTracking.pet, pet)
                .where(petTracking.tracking.id.eq(trackingId))
                .orderBy(pet.id.asc())
                .fetch();
    }

    private TrackingDetailSummary toTrackingDetailSummary(Tuple detailRow) {
        TrackingRoute route = detailRow.get(trackingRoute);
        return new TrackingDetailSummary(
                detailRow.get(tracking.id),
                detailRow.get(tracking.member.id),
                detailRow.get(tracking.startedAt),
                detailRow.get(tracking.endedAt),
                detailRow.get(tracking.duration),
                detailRow.get(tracking.visibility),
                detailRow.get(tracking.distance),
                detailRow.get(tracking.averagePace),
                route != null ? route.getOriginalPath() : List.of(),
                toDiarySummary(detailRow)
        );
    }

    private TrackingDetailSummary.DiarySummary toDiarySummary(Tuple detailRow) {
        UUID diaryId = detailRow.get(diary.id);
        if (diaryId == null) {
            return null;
        }

        return new TrackingDetailSummary.DiarySummary(
                diaryId,
                detailRow.get(diary.writingTime),
                detailRow.get(diary.title),
                detailRow.get(diary.content),
                detailRow.get(diary.temp),
                detailRow.get(diary.sky).getCode(),
                detailRow.get(diary.pty).getCode()
        );
    }

    @Override
    public List<UUID> findActiveMemberIds(LocalDateTime startDateTime, Pageable pageable) {
        return queryFactory
                .select(tracking.member.id)
                .distinct()
                .from(tracking)
                .where(tracking.startedAt.goe(startDateTime))
                .orderBy(tracking.member.id.asc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();
    }

    @Override
    public List<Tracking> findAllByMemberIdsAndDateRange(
            List<UUID> memberIds,
            LocalDateTime startDateTime
    ) {
        return queryFactory
                .selectFrom(tracking)
                .join(tracking.member, member).fetchJoin()
                .where(
                        tracking.member.id.in(memberIds),
                        tracking.startedAt.goe(startDateTime)
                )
                .fetch();
    }

    @Override
    public List<DailyTrackingSummary> getTrackingSummaryDateAsc(UUID memberId, LocalDate startDate, LocalDate endDate) {
        // DB의 날짜시간(LocalDateTime) 데이터를 날짜(LocalDate)로 캐스팅하기 위한 템플릿
        DateTemplate<java.sql.Date> datePath = Expressions.dateTemplate(
                java.sql.Date.class,
                "CAST({0} AS date)",
                tracking.startedAt
        );

        var distanceSumPath = tracking.distance.sum().coalesce(0);
        var durationSumPath = tracking.duration.sum().coalesce(0);
        var trackingCountPath = tracking.id.count();
        var restDurationSumPath = tracking.restDuration.sum().coalesce(0);

        // Tracking에서 memberId로 필터링 (startDate 00:00:00 ~ endDate 다음날 00:00:00 미만)
        List<Tuple> results = queryFactory
                .select(
                        datePath,
                        trackingCountPath,
                        distanceSumPath,
                        durationSumPath,
                        restDurationSumPath
                )
                .from(tracking)
                .where(
                        tracking.member.id.eq(memberId), // 멤버 ID 기준
                        tracking.startedAt.goe(startDate.atStartOfDay()),
                        tracking.startedAt.lt(endDate.plusDays(1).atStartOfDay())
                )
                .groupBy(datePath)
                .orderBy(datePath.asc())
                .fetch();

        // 조회가 편하도록 Map으로 변환
        Map<LocalDate, Tuple> dataMap = results.stream()
                .collect(Collectors.toMap(
                        tuple -> {
                            java.sql.Date sqlDate = tuple.get(datePath);
                            return sqlDate != null ? sqlDate.toLocalDate() : null;
                        },
                        tuple -> tuple
                ));

        // startDate 부터 endDate 까지의 날짜 스트림을 생성하여 빈 날짜도 0으로 매핑
        return startDate.datesUntil(endDate.plusDays(1))
                .map(currentDate -> {
                    Tuple tuple = dataMap.get(currentDate);

                    Integer count = tuple != null ? tuple.get(trackingCountPath).intValue() : 0;
                    Integer distance = tuple != null ? tuple.get(distanceSumPath) : 0;
                    Integer duration = tuple != null ? tuple.get(durationSumPath) : 0;
                    Integer restDuration = tuple != null ? tuple.get(restDurationSumPath) : 0;

                    return DailyTrackingSummary.builder()
                            .date(currentDate)
                            .trackingCount(count)
                            .distance(distance)
                            .duration(duration)
                            .restDuration(restDuration)
                            .build();
                })
                .toList();
    }

    @Override
    public List<PetActivityTracking> getPetActivitiesAsc(
            UUID memberId,
            UUID petId,
            LocalDate startDate,
            LocalDate endDate
    ) {
        return queryFactory
                .select(Projections.constructor(
                        PetActivityTracking.class,
                        pet.id,
                        pet.name,
                        pet.profileImageUrl,

                        tracking.id,
                        tracking.averagePace,
                        tracking.startedAt,
                        tracking.endedAt,
                        tracking.distance,
                        tracking.duration
                ))
                .from(petTracking)
                .join(petTracking.pet, pet)
                .join(petTracking.tracking, tracking)
                .where(
                        pet.id.eq(petId),
                        pet.member.id.eq(memberId),
                        tracking.member.id.eq(memberId),
                        tracking.startedAt.goe(startDate.atStartOfDay()),
                        tracking.startedAt.lt(endDate.atStartOfDay())
                )
                .orderBy(
                        tracking.startedAt.asc(),
                        tracking.id.asc()
                )
                .fetch();
    }


    @Override
    public List<DailyTracking> getDailyActivities(UUID memberId, LocalDate targetDate) {
        List<Tuple> results = queryFactory
                .select(tracking, diary.id)
                .from(tracking)
                .leftJoin(diary).on(diary.tracking.id.eq(tracking.id))
                .where(
                        tracking.member.id.eq(memberId),
                        tracking.startedAt.goe(targetDate.atStartOfDay()),
                        tracking.startedAt.lt(targetDate.plusDays(1).atStartOfDay())
                )
                .orderBy(tracking.startedAt.asc())
                .fetch();

        // DailyActivity DTO로 반환
        return results.stream().map(tuple -> {
            Tracking t = tuple.get(tracking);
            UUID diaryId = tuple.get(diary.id);

            return new DailyTracking(
                    t.getId(),
                    t.getStartedAt(),
                    t.getEndedAt(),
                    t.getDistance(),
                    t.getDuration(),
                    t.getAveragePace(),
                    diaryId,
                    t.getTrackingImages().stream()
                            .map(image -> new DailyTracking.TrackingImageSummary(
                                    image.getImageOrder(),
                                    image.getImageUrl()
                            ))
                            .toList()
            );
        }).toList();

    }

    @Override
    public List<DailyMemberStat> findMemberIdsByDate(List<UUID> memberIds, LocalDate startDate, LocalDate endDate) {
        return queryFactory
                .select(Projections.constructor(DailyMemberStat.class,
                        tracking.member.id,
                        tracking.id.count().intValue(),
                        tracking.distance.sum().coalesce(0),
                        tracking.duration.sum().coalesce(0)
                ))
                .from(tracking)
                .where(
                        tracking.startedAt.goe(startDate.atStartOfDay()),
                        tracking.startedAt.lt(endDate.plusDays(1).atStartOfDay()),
                        tracking.member.id.in(memberIds)
                )
                .groupBy(tracking.member.id)
                .fetch();
    }

}
