package org.zerock.puppyrun.tracking.repository;

import com.querydsl.core.Tuple;
import com.querydsl.core.types.dsl.DateTemplate;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.zerock.puppyrun.tracking.DTO.DailyTracking;
import org.zerock.puppyrun.tracking.DTO.DailyTrackingSummary;
import org.zerock.puppyrun.tracking.entity.Tracking;


import static org.zerock.puppyrun.diary.entity.QDiary.diary;
import static org.zerock.puppyrun.tracking.entity.QTracking.tracking;

@Repository
@RequiredArgsConstructor
public class TrackingRepoCustomImpl implements TrackingRepoCustom {
    private final JPAQueryFactory queryFactory;

    @Override
    public List<DailyTrackingSummary> getDayTracking(UUID memberId, LocalDate startDate, LocalDate endDate) {
        // DB의 날짜시간(LocalDateTime) 데이터를 날짜(LocalDate)로 캐스팅하기 위한 템플릿
        DateTemplate<java.sql.Date> datePath = Expressions.dateTemplate(
                java.sql.Date.class,
                "CAST({0} AS date)",
                tracking.startedAt
        );

        var distanceSumPath = tracking.distance.sum().coalesce(0);
        var durationSumPath = tracking.duration.sum().coalesce(0);

        // Tracking에서 memberId로 필터링 (startDate 00:00:00 ~ endDate 다음날 00:00:00 미만)
        List<Tuple> results = queryFactory
                .select(
                        datePath,
                        distanceSumPath,
                        durationSumPath
                )
                .from(tracking)
                .where(
                        tracking.member.id.eq(memberId), // 멤버 ID 기준
                        tracking.startedAt.goe(startDate.atStartOfDay()),
                        tracking.startedAt.lt(endDate.plusDays(1).atStartOfDay())
                )
                .groupBy(datePath)
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

                    return DailyTrackingSummary.builder()
                            .date(currentDate)
                            .trackingCount(count)
                            .distance(distance)
                            .duration(duration)
                            .build();
                })
                .toList();
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
                    t.getImages()
            );
        }).toList();

    }
}
