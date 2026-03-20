package org.zerock.puppyrun.tracking.repository;

import java.util.UUID;
import org.zerock.puppyrun.statistics.DTO.MemberTrackingSummary;

public interface TrackingRepoCustom {
    /**
     * 멤버(사용자)의 누적 거리, 누적 시간, 산책 횟수를 쿼리로 조회
     */
    public MemberTrackingSummary getTrackingSummaryByMemberId(UUID memberId);
}
