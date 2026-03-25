package org.zerock.puppyrun.tracking.DTO;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record DailyTracking(
        UUID trackingId,            // 산책 고유 ID (상세 페이지 이동용)
        LocalDateTime startedAt,    // 산책 시작 시간
        LocalDateTime endedAt,      // 산책 종료 시간
        Integer distance,          // 산책 거리
        Integer duration,        // 산책 시간
        String averagePace,         // 산책 페이스

        UUID diaryId,          // 일기 작성 여부 (UI 뱃지용)
        List<String> trackingImages // 산책 중 찍은 사진 리스트 (썸네일용)
) {

}
