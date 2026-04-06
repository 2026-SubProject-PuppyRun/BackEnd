package org.zerock.puppyrun.tracking.DTO;

import java.util.UUID;

public record DailyMemberStat(
        UUID memberId,
        Integer trackingCount, // 오늘 산책 횟수
        Integer totalDistance, // 오늘 총 걸은 거리
        Integer totalDuration  // 오늘 총 걸은 시간
) {
}
