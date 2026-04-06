package org.zerock.puppyrun.tracking.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.zerock.puppyrun.tracking.DTO.DailyMemberStat;
import org.zerock.puppyrun.tracking.DTO.DailyTracking;
import org.zerock.puppyrun.tracking.DTO.DailyTrackingSummary;

public interface TrackingRepoCustom {

    /**
     * 멤버의 특정 기간 동안의 일별 산책 누적 거리, 누적 시간, 산책 횟수를 쿼리로 조회
     */
    List<DailyTrackingSummary> getTrackingSummaryDateAsc(UUID memberId, LocalDate startDate, LocalDate endDate);


    List<DailyTracking> getDailyActivities(UUID memberId, LocalDate targetDate);

    List<DailyMemberStat> findMemberIdsByDate(List<UUID> memberIds, LocalDate startDate, LocalDate endDate);
}
