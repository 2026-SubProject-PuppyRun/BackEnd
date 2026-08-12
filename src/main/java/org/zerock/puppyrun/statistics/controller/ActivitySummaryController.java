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
import org.zerock.puppyrun.common.exception.InvalidValueException;
import org.zerock.puppyrun.statistics.controller.Response.DailyActivityResponse;
import org.zerock.puppyrun.statistics.controller.Response.MonthlyActivityResponse;
import org.zerock.puppyrun.statistics.controller.Response.MonthlyContributionResponse;
import org.zerock.puppyrun.statistics.controller.Response.PetActivityResponse;
import org.zerock.puppyrun.statistics.controller.Response.WeeklyActivityResponse;
import org.zerock.puppyrun.statistics.service.TrackingActivityService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/activity-tracking/statistics")
public class ActivitySummaryController {
    private final TrackingActivityService trackingActivityService;

    /**
     * 펫을 기준으로 마지막 활동량 통계를 조회합니다.
     */
    @GetMapping("/pet/last-tracking")
    public ResponseEntity<PetActivityResponse> getLastTracking(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam("startDate") LocalDate startDate,
            @RequestParam("endDate") LocalDate endDate
    ) {
        if (endDate.isBefore(startDate)) {
            throw new InvalidValueException("종료일은 시작일보다 빠를 수 없습니다.");
        }
        if (!endDate.isBefore(startDate.plusMonths(3))) {
            throw new InvalidValueException("3달 이상 조회는 불가능합니다.");
        }

        PetActivityResponse response = trackingActivityService.getPetLastActivity(principal, startDate, endDate);
        return ResponseEntity.ok(response);
    }


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
    public ResponseEntity<MonthlyActivityResponse> getMonthlyTracking(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam("date") LocalDate targetDay) {
        MonthlyActivityResponse response = trackingActivityService.getMonthlyTracking(principal, targetDay);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/monthly/contributions")
    public ResponseEntity<MonthlyContributionResponse> getMonthlyActivity(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam("date") LocalDate targetDay) {
        MonthlyContributionResponse response = trackingActivityService.getMonthlyContributions(principal, targetDay);
        return ResponseEntity.ok(response);

    }

}
