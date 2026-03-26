package org.zerock.puppyrun.statistics.service;

import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.zerock.puppyrun.common.auth.security.UserPrincipal;
import org.zerock.puppyrun.common.exception.ResourceNotFoundException;
import org.zerock.puppyrun.pet.entity.Pet;
import org.zerock.puppyrun.pet.repository.PetRepository;
import org.zerock.puppyrun.statistics.DTO.DailyPetTracking;
import org.zerock.puppyrun.statistics.controller.Response.DailyActivityResponse;
import org.zerock.puppyrun.tracking.DTO.TotalPetTracking;
import org.zerock.puppyrun.statistics.DTO.WeeklyActivityChart;
import org.zerock.puppyrun.statistics.controller.Response.WeeklyActivityResponse;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class TrackingActivityService {
    private final PetRepository petRepository;
    private final TrackingStatistics trackingStatistics;
    private final PetStatistics petStatistics;

    public DailyActivityResponse getDailyTracking(UserPrincipal principal, LocalDate targetDay) {
        List<DailyPetTracking> dailyPetTracking = trackingStatistics.getDayActivity(principal.id(), targetDay);
        return DailyActivityResponse.of(targetDay, dailyPetTracking);
    }

    public WeeklyActivityResponse getWeeklyTracking(UserPrincipal principal, LocalDate targetDay) {
        List<Pet> petList = petRepository.findAllByMemberId(principal.id());
        if (petList.isEmpty()) {
            throw new ResourceNotFoundException("펫이 존재하지 않습니다.");
        }

        // 일주일동안 산책한 통계
        WeeklyActivityChart activityChart = trackingStatistics.getWeeklyChart(principal.id(), targetDay);

        // 일주일동안 산책한 펫 통계
        List<TotalPetTracking> totalPetTracking = petStatistics.getWeeklyPetTrackingSummary(petList, targetDay);

        return WeeklyActivityResponse.of(activityChart, totalPetTracking);
    }

    public void getMonthlyTracking(UserPrincipal principal, LocalDate targetDay) {
    }
}
