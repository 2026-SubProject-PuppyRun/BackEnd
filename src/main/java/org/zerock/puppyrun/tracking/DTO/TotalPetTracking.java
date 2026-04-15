package org.zerock.puppyrun.tracking.DTO;

import java.time.LocalDate;
import java.util.UUID;
import lombok.Builder;
import org.zerock.puppyrun.pet.entity.PetBadge;

@Builder
public record TotalPetTracking(
        UUID petId,
        LocalDate startDate,
        LocalDate endDate,
        String name,            // 강아지 이름
        String profileImageUrl, // 프로필 이미지 URL
        String themeColor,      // 테마 색상 (엔티티의 color)
        PetBadge badge,         // 현재 뱃지
        Integer totalDistance,  // 누적 거리
        Integer totalDuration,  // 누적 시간
        Long totalCount,         // 산책 횟수
        Double averageSpeed,     // 평균 속도
        Integer restDuration      // 휴식 시간
) {
}

