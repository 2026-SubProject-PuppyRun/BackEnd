package org.zerock.puppyrun.statistics.service;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.zerock.puppyrun.common.auth.security.UserPrincipal;
import org.zerock.puppyrun.common.exception.ResourceNotFoundException;
import org.zerock.puppyrun.pet.repository.PetRepository;
import org.zerock.puppyrun.statistics.DTO.DailyPetTracking;
import org.zerock.puppyrun.statistics.DTO.MonthlyActivity;
import org.zerock.puppyrun.statistics.controller.Response.DailyActivityResponse;
import org.zerock.puppyrun.statistics.controller.Response.MonthlyActivityResponse;
import org.zerock.puppyrun.statistics.controller.Response.MonthlyContributionResponse;
import org.zerock.puppyrun.tracking.DTO.DailyTrackingSummary;
import org.zerock.puppyrun.tracking.DTO.TotalPetTracking;
import org.zerock.puppyrun.statistics.DTO.WeeklyActivityChart;
import org.zerock.puppyrun.statistics.controller.Response.WeeklyActivityResponse;
import org.zerock.puppyrun.tracking.repository.TrackingRepository;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class TrackingActivityService {
    private final PetRepository petRepository;
    private final TrackingStatistics trackingStatistics;
    private final PetStatistics petStatistics;
    private final TrackingRepository trackingRepository;


    public DailyActivityResponse getDailyTracking(UserPrincipal principal, LocalDate targetDay) {
        List<DailyPetTracking> dailyPetTracking = trackingStatistics.getDayActivity(principal.id(), targetDay);
        return DailyActivityResponse.of(targetDay, dailyPetTracking);
    }

    public WeeklyActivityResponse getWeeklyTracking(UserPrincipal principal, LocalDate targetDay) {
        List<UUID> petList = petRepository.findPetIdsByMemberId(principal.id());
        if (petList.isEmpty()) {
            throw new ResourceNotFoundException("펫이 존재하지 않습니다.");
        }

        // 1주 전 날짜
        LocalDate oneWeekAgo = targetDay.minusDays(6);

        // 일주일동안 산책한 통계
        CompletableFuture<WeeklyActivityChart> chartFuture = CompletableFuture.supplyAsync(
                () -> trackingStatistics.getWeeklyChart(principal.id(), oneWeekAgo, targetDay)
        );

        // 일주일동안 산책한 펫 통계
        CompletableFuture<List<TotalPetTracking>> thisWeekPetStatsFuture = CompletableFuture.supplyAsync(
                () -> petStatistics.getWeeklyPetTrackingSummary(petList, targetDay)
        );

        // 일주일전 동안 산책한 펫 통계
        CompletableFuture<List<TotalPetTracking>> lastWeekPetStatsFuture = CompletableFuture.supplyAsync(
                () -> petStatistics.getWeeklyPetTrackingSummary(petList, oneWeekAgo)
        );

        // 비동기 작업이 모두 끝날 때까지 대기 후 결과 병합
        CompletableFuture.allOf(chartFuture, thisWeekPetStatsFuture, lastWeekPetStatsFuture).join();
        WeeklyActivityChart activityChart = chartFuture.join();
        List<TotalPetTracking> thisWeekPetTracking = thisWeekPetStatsFuture.join();
        List<TotalPetTracking> lastWeekPetTracking = lastWeekPetStatsFuture.join();

        return WeeklyActivityResponse.of(activityChart, thisWeekPetTracking, lastWeekPetTracking, targetDay);
    }


    public MonthlyActivityResponse getMonthlyTracking(UserPrincipal principal, LocalDate targetDay) {
        CompletableFuture<List<MonthlyActivity>> monthlyRecordFuture = CompletableFuture.supplyAsync(
                () -> trackingStatistics.getMonthlyRecord(principal.id(), targetDay)
        );

        CompletableFuture<List<DailyTrackingSummary>> contributionFuture = CompletableFuture.supplyAsync(
                () -> trackingRepository.getTrackingSummaryDateAsc(principal.id(), targetDay.minusWeeks(15), targetDay)
        );

        return CompletableFuture.allOf(monthlyRecordFuture, contributionFuture)
                .thenApply(v -> {
                    List<MonthlyActivity> monthlyRecord = monthlyRecordFuture.join();
                    List<DailyTrackingSummary> contribution = contributionFuture.join();
                    return MonthlyActivityResponse.of(targetDay, monthlyRecord, contribution);
                })
                .join();
    }

    public MonthlyContributionResponse getMonthlyContributions(UserPrincipal principal, LocalDate targetDay) {
        MonthlyActivity activity = trackingStatistics.getMonthlyContribution(principal.id(), targetDay);
        return MonthlyContributionResponse.of(targetDay, activity);
    }


}
