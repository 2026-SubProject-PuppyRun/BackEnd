package org.zerock.puppyrun.statistics.service;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class TrackingStatistics {
    /**
     * 특정 펫의 누적 산책 거리(미터)를 조회합니다.
     */
    public int getTotalWalkedDistance(UUID petId) {
        // Todo: 추후 통계 개발 예정
        return 0;
    }
}
