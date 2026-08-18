package org.zerock.puppyrun.notification.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.zerock.puppyrun.common.logging.LogExecutionTime;
import org.zerock.puppyrun.notification.entity.WalkingPreference;
import org.zerock.puppyrun.notification.repository.WalkingPreferenceRepository;
import org.zerock.puppyrun.tracking.entity.RoutePoint;
import org.zerock.puppyrun.tracking.entity.Tracking;
import org.zerock.puppyrun.tracking.entity.TrackingRoute;
import org.zerock.puppyrun.tracking.repository.TrackingRepository;
import org.zerock.puppyrun.tracking.repository.TrackingRouteRepository;

/**
 * 최근 산책 기록을 분석하여 회원별 일일 선호 산책 시간대 스냅샷을 생성 및 적재하는 서비스입니다.
 * <p>
 * 주요 기능:
 * <ul>
 *   <li>최근 28일간의 회원별 산책 기록(Tracking)을 분석합니다.</li>
 *   <li>하루 24시간을 2시간 단위(총 12개 버킷)로 나누어 산책 시작 시각을 분류합니다.</li>
 *   <li>최근에 진행한 산책일수록 높은 가중치(1~4점)를 부여하여 평일/주말 선호 시간대를 결정합니다.</li>
 *   <li>대용량 데이터를 안전하게 처리하기 위해 청크(Chunk) 단위로 배치 분석 및 스냅샷을 적재합니다.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class WalkingPreferenceService {

    /**
     * 산책 기록 분석 대상 기간 (최근 28일)
     */
    private static final int ANALYSIS_WINDOW_DAYS = 28;

    /**
     * 시간대 분류 버킷 단위 (2시간 단위: 00시, 02시, 04시 ... 22시)
     */
    private static final int BUCKET_SIZE_HOURS = 2;

    /**
     * 배치 처리 시 한 번에 조회/분석할 회원 청크 크기
     */
    private static final int CHUNK_SIZE = 300;


    private final TrackingRepository trackingRepository;
    private final TrackingRouteRepository trackingRouteRepository;
    private final WalkingPreferenceRepository walkingPreferenceRepository;

    /**
     * 지정된 분석 시각(analysisAt)을 기준으로 회원별 산책 패턴을 분석하고 스냅샷을 저장합니다.
     * <p>
     * 1. 최근 28일 내 활동한 회원 ID 목록을 청크 단위로 조회합니다.<br>
     * 2. 이미 당일 스냅샷이 존재하는 회원은 분석 대상에서 제외합니다.<br>
     * 3. 분석 대상 회원의 산책 기록 데이터를 일괄 조회하여 평일/주말 버킷 점수를 산출합니다.<br>
     * 4. 생성된 선호도 스냅샷({@link WalkingPreference})을 저장합니다.
     *
     * @param analysisAt 분석 실행 시각
     */
    @LogExecutionTime
    public void createDailySnapshots(LocalDateTime analysisAt) {
        LocalDateTime startDateTime = analysisAt.minusDays(ANALYSIS_WINDOW_DAYS);
        LocalDate analysisDate = analysisAt.toLocalDate();
        int pageNumber = 0;
        int savedCount = 0;
        List<UUID> memberIds;

        log.info(
                "event=walking_preference_analysis_started, analysisAt={}, from={}, to={}, bucketSizeHours={}",
                analysisAt, startDateTime, analysisAt, BUCKET_SIZE_HOURS
        );
        do {
            Pageable pageable = PageRequest.of(pageNumber, CHUNK_SIZE);
            // 최근 28일 동안 활동 기록이 있는 회원 ID 페이지 조회
            memberIds = trackingRepository.findActiveMemberIds(startDateTime, analysisAt, pageable);
            if (memberIds.isEmpty()) {
                break;
            }

            // 당일 분석 스냅샷이 이미 존재하는 회원 조회 (중복 분석 방지)
            Map<UUID, WalkingPreference> existingSnapshots = walkingPreferenceRepository
                    .findAllByMemberIdInAndAnalysisDate(memberIds, analysisDate)
                    .stream()
                    .collect(Collectors.toMap(
                            snapshot -> snapshot.getMember().getId(),
                            snapshot -> snapshot
                    ));

            // 당일 스냅샷이 없는 미분석 회원 추출
            List<UUID> candidateMemberIds = memberIds.stream()
                    .filter(memberId -> !existingSnapshots.containsKey(memberId))
                    .toList();

            // 후보 회원들의 최근 28일간 산책 기록 조회
            List<Tracking> trackings = candidateMemberIds.isEmpty()
                    ? List.of()
                    : trackingRepository.findAllByMemberIdsAndDateRange(candidateMemberIds, startDateTime, analysisAt);

            // 전체 산책 기록 목록을 회원 ID별로 그룹화
            Map<UUID, List<Tracking>> trackingMap = trackings.stream()
                    .filter(tracking -> tracking.getMember().getId() != null)
                    .collect(Collectors.groupingBy(tracking -> tracking.getMember().getId()));

            // 회원별 산책 기록 목록에서 가장 최근의 산책 기록(Tracking) 1개씩만 추출
            Map<UUID, Tracking> latestTrackingMap = trackingMap.entrySet().stream()
                    .collect(Collectors.toMap(Entry::getKey, entry -> findLatestTracking(entry.getValue())));

            // 각 회원의 최신 산책 기록에 대한 경로(TrackingRoute) 데이터를 일괄 조회하여 Map으로 매핑
            Map<UUID, TrackingRoute> routeMap = trackingRouteRepository.findAllByTrackingIdIn(
                            latestTrackingMap.values().stream().map(Tracking::getId).toList()
                    )
                    .stream()
                    .collect(Collectors.toMap(TrackingRoute::getTrackingId, route -> route));

            // 각 회원별 스냅샷 객체 생성 및 null 필터링
            List<WalkingPreference> snapshots = candidateMemberIds.stream()
                    .map(memberId -> createSnapshot(
                            memberId,
                            trackingMap.getOrDefault(memberId, List.of()),
                            latestTrackingMap.get(memberId),
                            routeMap,
                            analysisAt,
                            analysisDate
                    ))
                    .filter(Objects::nonNull)
                    .toList();

            // 생성된 스냅샷 DB 저장
            if (!snapshots.isEmpty()) {
                walkingPreferenceRepository.saveAll(snapshots);
            }
            savedCount += snapshots.size();
            log.info(
                    "event=walking_preference_analysis_chunk_completed, page={}, memberCount={}, savedCount={}, skippedExistingCount={}",
                    pageNumber, memberIds.size(), snapshots.size(), existingSnapshots.size()
            );
            pageNumber++;
        } while (memberIds.size() == CHUNK_SIZE);

        log.info(
                "event=walking_preference_analysis_completed, analysisDate={}, savedCount={}",
                analysisDate, savedCount
        );
    }

    /**
     * 단일 회원의 산책 기록을 기반으로 평일/주말 선호 시간을 점수화하고 스냅샷 엔티티를 생성합니다.
     *
     * @param memberId     회원 ID
     * @param trackings    회원의 최근 산책 기록 리스트
     * @param analysisAt   분석 기준 시각
     * @param analysisDate 분석 기준 일자
     * @return 생성된 WalkingPreference 스냅샷 엔티티 (산책 기록이 없는 경우 null)
     */
    private WalkingPreference createSnapshot(
            UUID memberId,
            List<Tracking> trackings,
            Tracking latestTracking,
            Map<UUID, TrackingRoute> routeMap,
            LocalDateTime analysisAt,
            LocalDate analysisDate
    ) {
        if (trackings.isEmpty()) {
            log.debug("event=walking_preference_member_skipped, memberId={}, reason=no_tracking", memberId);
            return null;
        }

        // 평일 및 주말 시간대별 선호 점수 분석
        PreferenceScore weekdayScore = scoreByTimeBucket(trackings, analysisAt, false);
        PreferenceScore weekendScore = scoreByTimeBucket(trackings, analysisAt, true);
        LastWalkingLocation lastWalkingLocation = getLastWalkingLocation(latestTracking, routeMap);
        log.debug(
                "event=walking_preference_member_scored, memberId={}, trackingCount={}, weekdayBucketScores={}, weekdayPreferredTime={}, weekdayScore={}, weekendBucketScores={}, weekendPreferredTime={}, weekendScore={}",
                memberId,
                trackings.size(),
                weekdayScore.bucketScores(), weekdayScore.preferredTime(), weekdayScore.score(),
                weekendScore.bucketScores(), weekendScore.preferredTime(), weekendScore.score()
        );

        return WalkingPreference.builder()
                .member(trackings.getFirst().getMember())
                .analysisDate(analysisDate)
                .lastKnownLatitude(lastWalkingLocation.latitude())
                .lastKnownLongitude(lastWalkingLocation.longitude())
                .lastKnownDate(lastWalkingLocation.walkedDate())
                .weekdayTime(weekdayScore.preferredTime())
                .weekdayScore(weekdayScore.score())
                .weekendTime(weekendScore.preferredTime())
                .weekendScore(weekendScore.score())
                .build();
    }

    private Tracking findLatestTracking(List<Tracking> trackings) {
        return trackings.stream()
                .max(Comparator.comparing(Tracking::getStartedAt).thenComparing(Tracking::getId))
                .orElseThrow();
    }

    private LastWalkingLocation getLastWalkingLocation(
            Tracking latestTracking,
            Map<UUID, TrackingRoute> routeMap
    ) {
        TrackingRoute trackingRoute = routeMap.get(latestTracking.getId());
        if (trackingRoute == null || trackingRoute.getOriginalPath().isEmpty()) {
            return new LastWalkingLocation(null, null, latestTracking.getStartedAt().toLocalDate());
        }

        RoutePoint lastPoint = trackingRoute.getOriginalPath().getLast();
        return new LastWalkingLocation(lastPoint.lat(), lastPoint.lng(), latestTracking.getStartedAt().toLocalDate());
    }

    /**
     * 산책 기록들을 2시간 단위 시간대 버킷으로 분류하고 최근성 가중치를 누적하여 최고 선호 시간을 산출합니다.
     *
     * @param trackings  회원의 산책 기록 목록
     * @param analysisAt 분석 기준 시각
     * @param weekend    주말 여부 (true: 주말 산책 분석, false: 평일 산책 분석)
     * @return 버킷별 점수 분포와 최고 선호 시간을 담은 PreferenceScore
     */
    private PreferenceScore scoreByTimeBucket(
            List<Tracking> trackings,
            LocalDateTime analysisAt,
            boolean weekend
    ) {
        Map<Integer, ScoreAccumulator> scores = new HashMap<>();

        for (Tracking tracking : trackings) {
            LocalDateTime startedAt = tracking.getStartedAt();
            // 평일/주말 데이터 필터링
            if (isWeekend(startedAt.getDayOfWeek()) != weekend) {
                continue;
            }

            // 산책 시작 시간을 2시간 버킷 인덱스로 전환 (예: 14시 -> 7번 버킷)
            int bucketIndex = startedAt.getHour() / BUCKET_SIZE_HOURS;
            // 경과 일수에 따른 가중치 산출 (1~4점)
            int weight = calculateWeight(startedAt, analysisAt);
            // 해당 버킷에 점수 및 최근 산책 시작 시각 누적
            scores.computeIfAbsent(bucketIndex, ignored -> new ScoreAccumulator()).add(weight, startedAt);
        }

        // 전체 시간대 버킷별 점수 맵 생성 (LocalTime 기준 정렬)
        Map<LocalTime, Integer> bucketScores = scores.entrySet().stream()
                .sorted(Entry.comparingByKey())
                .collect(Collectors.toMap(
                        entry -> LocalTime.of(entry.getKey() * BUCKET_SIZE_HOURS, 0),
                        entry -> entry.getValue().score,
                        (left, right) -> left,
                        LinkedHashMap::new
                ));
        return getBestScore(scores, bucketScores);
    }

    /**
     * 요일(DayOfWeek)이 토요일 또는 일요일인지 확인합니다.
     */
    private boolean isWeekend(DayOfWeek dayOfWeek) {
        return dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY;
    }

    /**
     * 분석 시점 기준 산책 일자의 최근성(Recency)에 따른 가중치를 계산합니다.
     * <ul>
     *   <li>최근 7일 미만: 4점</li>
     *   <li>7일 이상 ~ 14일 미만: 3점</li>
     *   <li>14일 이상 ~ 21일 미만: 2점</li>
     *   <li>21일 이상 ~ 28일 미만: 1점</li>
     * </ul>
     */
    private int calculateWeight(LocalDateTime startedAt, LocalDateTime analysisAt) {
        long daysAgo = ChronoUnit.DAYS.between(startedAt.toLocalDate(), analysisAt.toLocalDate());
        if (daysAgo < 7) {
            return 4;
        }
        if (daysAgo < 14) {
            return 3;
        }
        if (daysAgo < 21) {
            return 2;
        }
        return 1;
    }

    /**
     * 집계된 버킷 점수 중 가장 높은 점수의 시간대(LocalTime)를 결정합니다.
     * <p>
     * **동점 우위 처리 규칙**:
     * <ol>
     *   <li>1순위: 버킷의 총 누적 점수 (최대점)</li>
     *   <li>2순위: 버킷 내 가장 최근에 진행된 산책 시각 (최신성)</li>
     *   <li>3순위: 더 늦은 시간대 버킷 (인덱스가 큰 버킷)</li>
     * </ol>
     */
    private PreferenceScore getBestScore(
            Map<Integer, ScoreAccumulator> scores,
            Map<LocalTime, Integer> bucketScores
    ) {
        return scores.entrySet().stream()
                .max(Comparator
                        .comparingInt((Entry<Integer, ScoreAccumulator> entry) -> entry.getValue().score)
                        .thenComparing(entry -> entry.getValue().latestStartedAt)
                        .thenComparing(Entry<Integer, ScoreAccumulator>::getKey, Comparator.reverseOrder()))
                .map(entry -> new PreferenceScore(
                        LocalTime.of(entry.getKey() * BUCKET_SIZE_HOURS, 0),
                        entry.getValue().score,
                        bucketScores
                ))
                .orElse(new PreferenceScore(null, null, bucketScores));
    }

    /**
     * 버킷 분석 결과(선호 시간, 점수, 전체 버킷별 점수 분포)를 담는 불변 객체입니다.
     */
    private record PreferenceScore(
            LocalTime preferredTime,
            Integer score,
            Map<LocalTime, Integer> bucketScores
    ) {
    }

    private record LastWalkingLocation(Double latitude, Double longitude, LocalDate walkedDate) {
    }

    /**
     * 시간대 버킷별 누적 점수와 가장 최근에 시작된 산책 시각을 집계하기 위한 헬퍼 클래스입니다.
     */
    private static class ScoreAccumulator {
        private int score;
        private LocalDateTime latestStartedAt;

        /**
         * 가중치를 누적하고, 더 최근의 산책 시각이면 latestStartedAt을 갱신합니다.
         */
        private void add(int weight, LocalDateTime startedAt) {
            score += weight;
            if (latestStartedAt == null || startedAt.isAfter(latestStartedAt)) {
                latestStartedAt = startedAt;
            }
        }
    }
}
