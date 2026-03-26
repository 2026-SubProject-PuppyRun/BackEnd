package org.zerock.puppyrun.tracking.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.zerock.puppyrun.tracking.DTO.DailyTracking;
import org.zerock.puppyrun.tracking.DTO.DailyTrackingSummary;
import org.zerock.puppyrun.tracking.DTO.MonthlyTrackingSummary;

public interface TrackingRepoCustom {

    /**
     * 멤버의 특정 기간 동안의 일별 산책 누적 거리, 누적 시간, 산책 횟수를 쿼리로 조회
     */
    List<DailyTrackingSummary> getDayTracking(UUID memberId, LocalDate startDate, LocalDate endDate);

    /**
     * 멤버의 월간 산책 통계 (해당 연도 1월부터 타겟 데이의 월까지)
     */
    List<MonthlyTrackingSummary> getMonthlyTracking(UUID memberId, LocalDate targetDay);

    List<DailyTracking> getDailyActivities(UUID memberId, LocalDate targetDate);
}
