package org.zerock.puppyrun.tracking.repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.zerock.puppyrun.statistics.DTO.TodayPetActivityTracking;
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
    List<MainTrackingSummary> findMainTrackingSummaries(UUID memberId);

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


    List<TodayPetActivityTracking> getPetActivities(UUID memberId, LocalDate startDate, LocalDate endDate);

    List<DailyTracking> getDailyActivities(UUID memberId, LocalDate targetDate);

    List<DailyMemberStat> findMemberIdsByDate(List<UUID> memberIds, LocalDate startDate, LocalDate endDate);
}
