package org.zerock.puppyrun.statistics.controller.Response;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.Builder;
import org.zerock.puppyrun.statistics.DTO.DailyPetTracking;

@Builder
public record DailyActivityResponse(
        LocalDate date,                 // 조회한 날짜
        DailySummary summary,           // 하루 전체 요약
        List<TrackingDetails> tracking  // 해당 날짜의 개별 산책 리스트
) {

    /**
     * DailyPetTracking 리스트를 기반으로 DailyActivityResponse를 생성하는 정적 팩토리 메서드
     */
    public static DailyActivityResponse of(LocalDate date, List<DailyPetTracking> dailyPetTrackings) {
        List<TrackingDetails> trackingDetailsList = dailyPetTrackings.stream()
                .map(TrackingDetails::from)
                .toList();

        return DailyActivityResponse.builder()
                .date(date)
                .summary(DailySummary.of(dailyPetTrackings))
                .tracking(trackingDetailsList)
                .build();
    }

    /**
     * 하루 전체 요약 스탯
     */
    @Builder
    public record DailySummary(
            Double totalDistanceKm,     // 하루 총 산책 거리 (km)
            Integer totalDurationMin,   // 하루 총 산책 시간 (분)
            Integer walkCount           // 하루 총 산책 횟수
    ) {
        public static DailySummary of(List<DailyPetTracking> dailyPetTrackings) {
            double totalDistance = dailyPetTrackings.stream()
                    .mapToDouble(DailyPetTracking::distance) // Integer를 Double로 캐스팅
                    .sum();
            int totalDuration = dailyPetTrackings.stream()
                    .mapToInt(DailyPetTracking::durationMin)
                    .sum();

            return DailySummary.builder()
                    .totalDistanceKm(totalDistance)
                    .totalDurationMin(totalDuration)
                    .walkCount(dailyPetTrackings.size())
                    .build();
        }
    }

    /**
     * 개별 산책 상세 정보
     */
    @Builder
    public record TrackingDetails(
            UUID trackingId,            // 산책 고유 ID (상세 페이지 이동용)
            LocalDateTime startedAt,    // 산책 시작 시간
            LocalDateTime endedAt,      // 산책 종료 시간
            Double distanceKm,          // 산책 거리 (km)
            Integer durationMin,        // 산책 시간 (분)
            String averagePace,         // 산책 페이스

            DiaryDetail diary,          // 일기 작성 여부 (UI 뱃지용)
            List<String> trackingImages, // 산책 중 찍은 사진 리스트 (썸네일용)
            List<ParticipatingPet> participatingPets // 참여한 펫 목록
    ) {
        public static TrackingDetails from(DailyPetTracking dpt) {
            return TrackingDetails.builder()
                    .trackingId(dpt.trackingId())
                    .startedAt(dpt.startedAt())
                    .endedAt(dpt.endedAt())
                    .distanceKm((double) dpt.distance()) // Integer -> Double 변환
                    .durationMin(dpt.durationMin())
                    .averagePace(dpt.averagePace())
                    .diary(DiaryDetail.from(dpt.diary()))
                    .trackingImages(dpt.trackingImages())
                    .participatingPets(dpt.participatingPets().stream()
                            .map(ParticipatingPet::from)
                            .toList())
                    .build();
        }
    }

    @Builder
    public record DiaryDetail(
            boolean hasDiary,           // 일기 작성 여부 (UI 뱃지용)
            UUID diaryId               // 작성된 일기가 있다면 일기 ID (없으면 null)
    ) {
        public static DiaryDetail from(DailyPetTracking.DiaryDetail diaryDetail) {
            return DiaryDetail.builder()
                    .hasDiary(diaryDetail.hasDiary())
                    .diaryId(diaryDetail.diaryId())
                    .build();
        }
    }

    /**
     * 산책에 참여한 펫 정보
     */
    @Builder
    public record ParticipatingPet(
            UUID petId,                 // 펫 고유 ID
            String name,                // 펫 이름
            String profileImageUrl,     // 펫 프로필 이미지
            String themeColor           // 펫 고유 색상 (UI 테두리 등에 활용)
    ) {
        public static ParticipatingPet from(DailyPetTracking.ParticipatingPet pet) {
            return ParticipatingPet.builder()
                    .petId(pet.petId())
                    .name(pet.name())
                    .profileImageUrl(pet.profileImageUrl())
                    .themeColor(pet.themeColor())
                    .build();
        }
    }
}
