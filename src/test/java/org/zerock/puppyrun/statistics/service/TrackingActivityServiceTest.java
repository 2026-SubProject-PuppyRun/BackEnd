package org.zerock.puppyrun.statistics.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
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
import org.zerock.puppyrun.pet.entity.Pet;
import org.zerock.puppyrun.pet.entity.PetBadge;
import org.zerock.puppyrun.pet.repository.PetRepository;
import org.zerock.puppyrun.statistics.DTO.DailyPetTracking;
import org.zerock.puppyrun.statistics.DTO.WeeklyActivityChart;
import org.zerock.puppyrun.statistics.controller.Response.DailyActivityResponse;
import org.zerock.puppyrun.statistics.controller.Response.WeeklyActivityResponse;
import org.zerock.puppyrun.tracking.DTO.TotalPetTracking;

@ExtendWith(MockitoExtension.class)
class TrackingActivityServiceTest {

    @Mock
    private PetRepository petRepository;

    @Mock
    private TrackingStatistics trackingStatistics;

    @Mock
    private PetStatistics petStatistics;

    @InjectMocks
    private TrackingActivityService trackingActivityService;

    private UserPrincipal principal;
    private LocalDate targetDay;
    private UUID memberId;

    @BeforeEach
    void setUp() {
        memberId = UUID.randomUUID();
        principal = new UserPrincipal(memberId, "test@puppyrun.com", UserRole.USER);
        targetDay = LocalDate.of(2026, 3, 23); // 임의의 테스트 기준일
    }

    // ==========================================
    // 1. getDailyTracking 테스트
    // ==========================================

    @Test
    @DisplayName("하루 산책 상세 내역 조회 - 데이터가 있을 경우 정상적으로 응답 객체가 생성된다.")
    void getDailyTracking_Success_WithData() {
        // given
        UUID trackingId = UUID.randomUUID();
        DailyPetTracking dummyTracking = createDummyDailyPetTracking(trackingId);

        when(trackingStatistics.getDayActivity(memberId, targetDay))
                .thenReturn(List.of(dummyTracking));

        // when
        DailyActivityResponse response = trackingActivityService.getDailyTracking(principal, targetDay);

        // then
        assertNotNull(response);
        assertEquals(targetDay, response.date());

        // Summary 검증 (더미 데이터: 3km, 60분)
        assertEquals(3.0, response.summary().totalDistanceKm());
        assertEquals(60, response.summary().totalDurationMin());
        assertEquals(1, response.summary().walkCount());

        // 개별 상세 리스트 검증
        assertEquals(1, response.tracking().size());
        assertEquals(trackingId, response.tracking().getFirst().trackingId());
    }

    @Test
    @DisplayName("하루 산책 상세 내역 조회 - 산책 기록이 없으면 빈 리스트와 0 스탯을 반환한다.")
    void getDailyTracking_Success_EmptyData() {
        // given
        when(trackingStatistics.getDayActivity(memberId, targetDay))
                .thenReturn(Collections.emptyList());

        // when
        DailyActivityResponse response = trackingActivityService.getDailyTracking(principal, targetDay);

        // then
        assertNotNull(response);
        assertEquals(0.0, response.summary().totalDistanceKm());
        assertEquals(0, response.summary().walkCount());
        assertTrue(response.tracking().isEmpty());
    }

    // ==========================================
    // 2. getWeeklyTracking 테스트
    // ==========================================

    @Test
    @DisplayName("주간 통계 조회 - 등록된 펫이 있고 통계가 정상적으로 반환된다.")
    void getWeeklyTracking_Success() {
        // given
        Pet mockPet = mock(Pet.class);
        UUID petId = UUID.randomUUID();
        List<Pet> petList = List.of(mockPet);

        when(petRepository.findAllByMemberId(memberId)).thenReturn(petList);

        // 차트 데이터 세팅 (원시 데이터: 3000m, 3600초)
        WeeklyActivityChart dummyChart = createDummyWeeklyActivityChart();
        when(trackingStatistics.getWeeklyChart(memberId, targetDay)).thenReturn(dummyChart);

        // 펫 통계 데이터 세팅 (원시 데이터: 3000m, 3600초)
        TotalPetTracking dummySummary = createDummyTotalPetTracking(petId);
        when(petStatistics.getWeeklyPetTrackingSummary(petList, targetDay)).thenReturn(List.of(dummySummary));

        // when
        WeeklyActivityResponse response = trackingActivityService.getWeeklyTracking(principal, targetDay);

        // then
        assertNotNull(response);
        assertEquals("weekly", response.period().type());

        // 전체 요약 검증 (거리: 3000m -> 3.0km, 시간: 3600초 -> 60분)
        assertEquals(3.0, response.summary().totalDistanceKm());
        assertEquals(60, response.summary().totalDurationMin());

        // 패밀리 리포트(펫 통계) 검증
        assertEquals(1, response.familyReport().totalDogs());
        assertEquals(petId, response.familyReport().dogStats().getFirst().dogId());
        assertEquals(100.0, response.familyReport().dogStats().getFirst().sharePercentage()); // 1마리이므로 100%
        assertEquals("000", response.familyReport().dogStats().getFirst().badge());
    }

    @Test
    @DisplayName("주간 통계 조회 - 회원이 등록한 펫이 없을 경우 ResourceNotFoundException 예외가 발생한다.")
    void getWeeklyTracking_Fail_NoPets() {
        // given
        when(petRepository.findAllByMemberId(memberId)).thenReturn(Collections.emptyList());

        // when & then
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            trackingActivityService.getWeeklyTracking(principal, targetDay);
        });

        assertEquals("펫이 존재하지 않습니다.", exception.getMessage());

        // 예외 발생 시 하위 서비스 로직들은 호출되지 않아야 함
        verify(trackingStatistics, never()).getWeeklyChart(any(), any());
        verify(petStatistics, never()).getWeeklyPetTrackingSummary(any(), any());
    }

    // ==========================================
    // 테스트용 헬퍼 메서드 (더미 객체 생성)
    // ==========================================

    private DailyPetTracking createDummyDailyPetTracking(UUID trackingId) {
        DailyPetTracking.DiaryDetail diaryDetail = DailyPetTracking.DiaryDetail.builder()
                .hasDiary(true)
                .diaryId(UUID.randomUUID())
                .build();

        DailyPetTracking.ParticipatingPet petDetail = DailyPetTracking.ParticipatingPet.builder()
                .petId(UUID.randomUUID())
                .name("보리")
                .themeColor("#FFFFFF")
                .profileImageUrl("http://image.url")
                .build();

        return new DailyPetTracking(
                trackingId,
                targetDay.atStartOfDay().plusHours(10),
                targetDay.atStartOfDay().plusHours(11),
                3,       // distance (km)
                60,      // durationMin
                "20'00\"",
                diaryDetail,
                List.of("image1.jpg"),
                List.of(petDetail)
        );
    }

    private WeeklyActivityChart createDummyWeeklyActivityChart() {
        WeeklyActivityChart.ActivityChart activityChart = WeeklyActivityChart.ActivityChart.builder()
                .date(targetDay)
                .label("MONDAY")
                .distance(3000) // m 단위
                .duration(3600) // 초 단위
                .build();

        return new WeeklyActivityChart(
                targetDay.minusDays(6),
                targetDay,
                List.of(activityChart)
        );
    }

    private TotalPetTracking createDummyTotalPetTracking(UUID petId) {
        return new TotalPetTracking(
                petId,
                "보리",
                "http://image.url",
                "#FFFFFF",
                PetBadge.BEGINNER,
                3000, // totalDistance (m)
                3600, // totalDuration (초)
                1L    // totalCount
        );
    }
}
