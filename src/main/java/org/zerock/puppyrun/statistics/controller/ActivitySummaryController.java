package org.zerock.puppyrun.statistics.controller;


import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.zerock.puppyrun.common.auth.security.UserPrincipal;
import org.zerock.puppyrun.statistics.controller.Response.DailyActivityResponse;
import org.zerock.puppyrun.statistics.controller.Response.WeeklyActivityResponse;
import org.zerock.puppyrun.statistics.service.TrackingActivityService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/activity-tracking/statistics")
public class ActivitySummaryController {
    private final TrackingActivityService trackingActivityService;

    @GetMapping("/daily")
    public ResponseEntity<DailyActivityResponse> getDailyTracking(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam("date") LocalDate targetDay) {
        DailyActivityResponse response = trackingActivityService.getDailyTracking(principal, targetDay);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/weekly")
    public ResponseEntity<WeeklyActivityResponse> getWeeklyTracking(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam("date") LocalDate targetDay) {

        WeeklyActivityResponse response = trackingActivityService.getWeeklyTracking(principal, targetDay);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/monthly")
    public ResponseEntity<?> getMonthlyTracking(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam("date") LocalDate targetDay) {
        // todo: 잔디 심기, 월간 산책 통계 구현 예정
        return ResponseEntity.ok().build();
    }

}
