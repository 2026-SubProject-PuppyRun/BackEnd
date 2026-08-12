package org.zerock.puppyrun.statistics.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.BDDMockito.given;

import java.time.LocalDate;
import java.time.Month;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.zerock.puppyrun.common.auth.security.UserPrincipal;
import org.zerock.puppyrun.common.exception.ResourceNotFoundException;
import org.zerock.puppyrun.member.entity.UserRole;
import org.zerock.puppyrun.pet.repository.PetRepository;
import org.zerock.puppyrun.statistics.DTO.DailyPetTracking;
import org.zerock.puppyrun.statistics.DTO.MonthlyActivity;
import org.zerock.puppyrun.statistics.DTO.WeeklyActivityChart;
import org.zerock.puppyrun.statistics.controller.Response.DailyActivityResponse;
import org.zerock.puppyrun.statistics.controller.Response.MonthlyActivityResponse;
import org.zerock.puppyrun.statistics.controller.Response.MonthlyContributionResponse;
import org.zerock.puppyrun.statistics.controller.Response.WeeklyActivityResponse;
import org.zerock.puppyrun.tracking.DTO.DailyTrackingSummary;
import org.zerock.puppyrun.tracking.DTO.DailyTracking;
import org.zerock.puppyrun.tracking.DTO.TotalPetTracking;
import org.zerock.puppyrun.tracking.repository.TrackingRepository;

@ExtendWith(MockitoExtension.class)
class TrackingActivityServiceTest {

    @Mock
    private PetRepository petRepository;

    @Mock
    private TrackingStatistics trackingStatistics;

    @Mock
    private PetStatistics petStatistics;

    @Mock
    private TrackingRepository trackingRepository;

    @InjectMocks
    private TrackingActivityService trackingActivityService;

    private UserPrincipal principal;
    private LocalDate targetDay;
    private UUID memberId;

    @BeforeEach
    void setUp() {
        memberId = UUID.randomUUID();
        principal = new UserPrincipal(memberId, "test@puppyrun.com", UserRole.USER);
        targetDay = LocalDate.of(2026, 3, 23);
    }

    @Test
    @DisplayName("하루 산책 기록을 거리·시간 요약과 상세 내역으로 조회한다")
    void getDailyTracking() {
        // given
        UUID trackingId = UUID.randomUUID();
        given(trackingStatistics.getDayActivity(memberId, targetDay))
                .willReturn(List.of(createDailyTracking(trackingId)));

        // when
        DailyActivityResponse response = trackingActivityService.getDailyTracking(principal, targetDay);

        // then
        assertThat(response.date()).isEqualTo(targetDay);
        assertThat(response.summary().totalDistanceM()).isEqualTo(3_000);
        assertThat(response.summary().totalDurationSec()).isEqualTo(3_600);
        assertThat(response.summary().walkCount()).isEqualTo(1);
        assertThat(response.tracking())
                .singleElement()
                .extracting(DailyActivityResponse.TrackingDetails::trackingId)
                .isEqualTo(trackingId);
        assertThat(response.tracking().getFirst().trackingImages())
                .extracting(DailyActivityResponse.TrackingImage::order,
                        DailyActivityResponse.TrackingImage::image)
                .containsExactly(tuple(0, "image1.jpg"));
    }

    @Test
    @DisplayName("산책 기록이 없는 날은 0으로 집계된 빈 내역을 반환한다")
    void getEmptyDailyTracking() {
        // given
        given(trackingStatistics.getDayActivity(memberId, targetDay)).willReturn(List.of());

        // when
        DailyActivityResponse response = trackingActivityService.getDailyTracking(principal, targetDay);

        // then
        assertThat(response.summary().totalDistanceM()).isZero();
        assertThat(response.summary().totalDurationSec()).isZero();
        assertThat(response.summary().walkCount()).isZero();
        assertThat(response.tracking()).isEmpty();
    }

    @Test
    @DisplayName("주간 산책 기록을 전체 요약·반려견별 비중·이전 주 비교 통계로 반환한다")
    void getWeeklyTrackingStatistics() {
        // given
        LocalDate weekStart = targetDay.minusDays(6);
        UUID firstPetId = UUID.randomUUID();
        UUID secondPetId = UUID.randomUUID();
        List<UUID> petIds = List.of(firstPetId, secondPetId);
        WeeklyActivityChart weeklyChart = new WeeklyActivityChart(
                weekStart,
                targetDay,
                List.of(
                        weeklyChart(weekStart, "TUESDAY", 1_200, 1_800),
                        weeklyChart(weekStart.plusDays(1), "WEDNESDAY", 0, 0),
                        weeklyChart(targetDay, "MONDAY", 2_300, 2_700)
                )
        );
        List<TotalPetTracking> thisWeek = List.of(
                petTracking(firstPetId, "보리", targetDay, 3_000, 3_600, 2L, 6.5, 600),
                petTracking(secondPetId, "두부", targetDay, 1_000, 1_800, 1L, 4.5, 300)
        );
        List<TotalPetTracking> lastWeek = List.of(
                petTracking(firstPetId, "보리", weekStart, 2_000, 2_400, 1L, 5.0, 300),
                petTracking(secondPetId, "두부", weekStart, 500, 900, 1L, 3.5, 120)
        );
        given(petRepository.findPetIdsByMemberId(memberId)).willReturn(petIds);
        given(trackingStatistics.getWeeklyChart(memberId, weekStart, targetDay)).willReturn(weeklyChart);
        given(petStatistics.getWeeklyPetTrackingSummary(petIds, targetDay)).willReturn(thisWeek);
        given(petStatistics.getWeeklyPetTrackingSummary(petIds, weekStart)).willReturn(lastWeek);

        // when
        WeeklyActivityResponse response = trackingActivityService.getWeeklyTracking(principal, targetDay);

        // then
        assertThat(response.period().type()).isEqualTo("weekly");
        assertThat(response.period().startDate()).isEqualTo(weekStart);
        assertThat(response.period().endDate()).isEqualTo(targetDay);
        assertThat(response.summary().totalDistanceM()).isEqualTo(3_500);
        assertThat(response.summary().totalDurationSec()).isEqualTo(4_500);
        assertThat(response.summary().totalCount()).isEqualTo(2);
        assertThat(response.activityChart())
                .extracting(WeeklyActivityResponse.ActivityChart::distanceM)
                .containsExactly(1_200, 0, 2_300);
        assertThat(response.familyReport().totalDogs()).isEqualTo(2);
        assertThat(response.familyReport().dogStats())
                .extracting(WeeklyActivityResponse.DogStat::sharePercentage)
                .containsExactly(75.0, 25.0);

        WeeklyActivityResponse.DogRadar firstDogRadar = response.dogRadars().getFirst();
        assertThat(firstDogRadar.dogId()).isEqualTo(firstPetId);
        assertThat(firstDogRadar.dataPoints().getFirst().metricCode()).isEqualTo("DISTANCE");
        assertThat(firstDogRadar.dataPoints().getFirst().thisWeekValue()).isEqualTo(3_000.0);
        assertThat(firstDogRadar.dataPoints().getFirst().lastWeekValue()).isEqualTo(2_000.0);
    }

    @Test
    @DisplayName("등록한 반려견이 없으면 주간 산책 통계를 조회할 수 없다")
    void rejectWeeklyTrackingWithoutPet() {
        // given
        given(petRepository.findPetIdsByMemberId(memberId)).willReturn(List.of());

        // when
        Throwable thrown = org.assertj.core.api.Assertions.catchThrowable(
                () -> trackingActivityService.getWeeklyTracking(principal, targetDay)
        );

        // then
        assertThat(thrown)
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("펫이 존재하지 않습니다.");
    }

    @Test
    @DisplayName("월별 산책 요약과 최근 15주 및 선택한 월의 일별 통계를 정확히 반환한다")
    void getMonthlyTrackingStatistics() {
        // given
        LocalDate firstDay = LocalDate.of(2026, 3, 1);
        LocalDate secondDay = LocalDate.of(2026, 3, 2);
        MonthlyActivity january = MonthlyActivity.builder()
                .month(Month.JANUARY)
                .trackingCount(2)
                .totalDistance(1_250)
                .totalDuration(1_800)
                .activityChart(List.of())
                .build();
        MonthlyActivity march = MonthlyActivity.builder()
                .month(Month.MARCH)
                .trackingCount(3)
                .totalDistance(2_750)
                .totalDuration(3_600)
                .activityChart(List.of(
                        monthlyChart(firstDay, 1, 1_200, 1_800),
                        monthlyChart(secondDay, 2, 1_550, 1_800)
                ))
                .build();
        List<DailyTrackingSummary> contributions = List.of(
                dailySummary(firstDay, 1, 1_200, 1_800),
                dailySummary(secondDay, 2, 1_550, 1_800)
        );
        given(trackingStatistics.getMonthlyRecord(memberId, targetDay))
                .willReturn(List.of(january, march));
        given(trackingRepository.getTrackingSummaryDateAsc(
                memberId,
                targetDay.minusWeeks(15),
                targetDay
        )).willReturn(contributions);
        given(trackingStatistics.getMonthlyContribution(memberId, targetDay)).willReturn(march);

        // when
        MonthlyActivityResponse overview =
                trackingActivityService.getMonthlyTracking(principal, targetDay);
        MonthlyContributionResponse daily =
                trackingActivityService.getMonthlyContributions(principal, targetDay);

        // then
        assertThat(overview.period().type()).isEqualTo("monthly");
        assertThat(overview.period().year()).isEqualTo("2026");
        assertThat(overview.monthlySummary()).hasSize(2);
        assertThat(overview.monthlySummary().getFirst().label()).isEqualTo("JANUARY");
        assertThat(overview.monthlySummary().getFirst().totalDistanceM()).isEqualTo(1_250);
        assertThat(overview.monthlySummary().getFirst().totalDurationSec()).isEqualTo(1_800);
        assertThat(overview.monthlySummary().getFirst().totalCount()).isEqualTo(2);
        assertThat(overview.monthlySummary().get(1).label()).isEqualTo("MARCH");
        assertThat(overview.monthlySummary().get(1).totalDistanceM()).isEqualTo(2_750);
        assertThat(overview.monthlySummary().get(1).totalDurationSec()).isEqualTo(3_600);
        assertThat(overview.monthlySummary().get(1).totalCount()).isEqualTo(3);
        assertThat(overview.contributionChart())
                .extracting(MonthlyActivityResponse.ContributionChart::distanceM)
                .containsExactly(1_200, 1_550);

        assertThat(daily.period().type()).isEqualTo("contributions");
        assertThat(daily.period().month()).isEqualTo("MARCH");
        assertThat(daily.activityChart()).hasSize(2);
        assertThat(daily.activityChart().getFirst().label()).isEqualTo(firstDay);
        assertThat(daily.activityChart().getFirst().distanceM()).isEqualTo(1_200);
        assertThat(daily.activityChart().getFirst().durationSec()).isEqualTo(1_800);
        assertThat(daily.activityChart().getFirst().trackingCount()).isEqualTo(1);
    }

    private DailyPetTracking createDailyTracking(UUID trackingId) {
        DailyPetTracking.DiaryDetail diary = DailyPetTracking.DiaryDetail.builder()
                .hasDiary(true)
                .diaryId(UUID.randomUUID())
                .build();
        DailyPetTracking.ParticipatingPet pet = DailyPetTracking.ParticipatingPet.builder()
                .petId(UUID.randomUUID())
                .name("보리")
                .themeColor("#FFFFFF")
                .profileImageUrl("http://image.url")
                .build();

        return new DailyPetTracking(
                trackingId,
                targetDay.atTime(10, 0),
                targetDay.atTime(11, 0),
                3_000,
                3_600,
                12000.0,
                diary,
                List.of(new DailyPetTracking.TrackingImageSummary(0, "image1.jpg")),
                List.of(pet)
        );
    }

    private WeeklyActivityChart.ActivityChart weeklyChart(
            LocalDate date,
            String label,
            int distance,
            int duration
    ) {
        return WeeklyActivityChart.ActivityChart.builder()
                .date(date)
                .label(label)
                .distance(distance)
                .duration(duration)
                .restDuration(0)
                .build();
    }

    private TotalPetTracking petTracking(
            UUID petId,
            String name,
            LocalDate endDate,
            int distance,
            int duration,
            long count,
            double averageSpeed,
            int restDuration
    ) {
        return TotalPetTracking.builder()
                .petId(petId)
                .startDate(endDate.minusDays(6))
                .endDate(endDate)
                .name(name)
                .profileImageUrl("https://image.test/" + petId)
                .themeColor("#FFFFFF")
                .walkedDistance(0)
                .totalDistance(distance)
                .totalDuration(duration)
                .totalCount(count)
                .averageSpeed(averageSpeed)
                .restDuration(restDuration)
                .build();
    }

    private MonthlyActivity.ActivityChart monthlyChart(
            LocalDate date,
            int trackingCount,
            int distance,
            int duration
    ) {
        return MonthlyActivity.ActivityChart.builder()
                .date(date)
                .trackingCount(trackingCount)
                .totalDistance(distance)
                .totalDuration(duration)
                .build();
    }

    private DailyTrackingSummary dailySummary(
            LocalDate date,
            int trackingCount,
            int distance,
            int duration
    ) {
        return DailyTrackingSummary.builder()
                .date(date)
                .trackingCount(trackingCount)
                .distance(distance)
                .duration(duration)
                .restDuration(0)
                .build();
    }
}
