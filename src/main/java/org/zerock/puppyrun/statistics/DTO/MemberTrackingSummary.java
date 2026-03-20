package org.zerock.puppyrun.statistics.DTO;

public record MemberTrackingSummary(
        Integer totalDistance,  // 누적 거리
        Integer totalDuration,  // 누적 시간
        Long totalCount      // 산책 횟수
) {
}
