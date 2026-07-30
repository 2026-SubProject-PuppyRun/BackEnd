package org.zerock.puppyrun.common.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.zerock.puppyrun.notification.service.WalkingPreferenceService;

/**
 * 회원별 산책 선호 시간 분석을 정기 실행합니다.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class WalkingPreferenceScheduler {

    private final WalkingPreferenceService walkingPreferenceService;

    /**
     * 매일 오전 3시에 최근 산책 기록을 분석합니다.
     */
    @Scheduled(cron = "0 0 3 * * *")
    public void scheduledPreferenceUpdate() {
        log.info("정기 산책 패턴 분석을 시작합니다.");
        try {
            walkingPreferenceService.updateAllMemberPreferences();
        } catch (Exception exception) {
            log.error("산책 패턴 분석 중 오류가 발생했습니다.", exception);
        }
    }
}
