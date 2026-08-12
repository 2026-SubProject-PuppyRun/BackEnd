package org.zerock.puppyrun.tracking.repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.zerock.puppyrun.statistics.DTO.PetActivityTracking;
import org.zerock.puppyrun.tracking.DTO.DailyMemberStat;
import org.zerock.puppyrun.tracking.DTO.DailyTracking;
import org.zerock.puppyrun.tracking.DTO.DailyTrackingSummary;
import org.zerock.puppyrun.tracking.DTO.MainTrackingSummary;
import org.zerock.puppyrun.tracking.DTO.TrackingDetailSummary;
import org.zerock.puppyrun.tracking.entity.Tracking;

public interface TrackingRepoCustom {

    /**
     * 회원의 산책 목록을 대표 이미지, 경로와 함께 조회합니다.
     */
    MainTrackingSummary findMainTrackingSummaries(UUID memberId, Pageable pageable);

    /**
     * 산책 상세 기본 정보와 경로 및 일기를 조회합니다.
     */
    Optional<TrackingDetailSummary> findTrackingDetailSummary(UUID trackingId);

    /**
     * 산책 이미지를 등록 순서대로 조회합니다.
     */
    List<TrackingDetailSummary.TrackingImageSummary> findTrackingImageSummaries(UUID trackingId);

    /**
     * 산책에 참여한 펫을 조회합니다.
     */
    List<TrackingDetailSummary.ParticipatingPet> findParticipatingPetSummaries(UUID trackingId);

    /**
     * 기준 시각 이후 산책한 회원 ID를 페이지 단위로 조회합니다.
     */
    List<UUID> findActiveMemberIds(LocalDateTime startDateTime, Pageable pageable);

    /**
     * 여러 회원의 기준 시각 이후 산책 기록을 회원과 함께 조회합니다.
     */
    List<Tracking> findAllByMemberIdsAndDateRange(List<UUID> memberIds, LocalDateTime startDateTime);

    /**
     * 멤버의 특정 기간 동안의 일별 산책 누적 거리, 누적 시간, 산책 횟수를 쿼리로 조회
     */
    List<DailyTrackingSummary> getTrackingSummaryDateAsc(UUID memberId, LocalDate startDate, LocalDate endDate);

    /**
     * 특정 회원의 반려동물이 지정된 기간에 수행한 산책 기록을 조회합니다.
     *
     * <p>조회 기간은 {@code startDate} 00:00 이상부터 {@code endDate} 다음 날 00:00 미만까지이며,
     * 산책 시작 시각 기준 오름차순으로 반환합니다.</p>
     *
     * @return 해당 반려동물의 기간 내 산책 기록 목록. 기록이 없으면 빈 목록
     */
    List<PetActivityTracking> getPetActivitiesAsc(UUID memberId, UUID petId, LocalDate startDate, LocalDate endDate);

    /**
     * 특정 회원의 지정 날짜 산책 기록을 조회합니다.
     *
     * <p>조회 범위는 {@code targetDate} 00:00 이상부터 다음 날 00:00 미만까지입니다.</p>
     *
     * @return 해당 날짜의 산책 기록 목록. 기록이 없으면 빈 목록
     */
    List<DailyTracking> getDailyActivities(UUID memberId, LocalDate targetDate);

    /**
     * 여러 회원의 지정 기간 산책 통계를 회원별로 집계합니다.
     *
     * <p>회원별 산책 횟수, 총 거리 및 총 시간을 반환합니다.
     * 조회 기간은 {@code startDate} 00:00 이상부터 {@code endDate} 다음 날 00:00 미만까지입니다.</p>
     *
     * @return 기간 내 산책 기록이 있는 회원별 집계 목록
     */
    List<DailyMemberStat> findMemberIdsByDate(List<UUID> memberIds, LocalDate startDate, LocalDate endDate);
}
