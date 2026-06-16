package org.zerock.puppyrun.gamification.controller.response;

import java.util.List;
import java.util.UUID;
import lombok.Builder;
import org.zerock.puppyrun.gamification.DTO.LevelInfo;
import org.zerock.puppyrun.pet.entity.PetBadge;
import org.zerock.puppyrun.tracking.DTO.TotalMemberTracking;
import org.zerock.puppyrun.tracking.DTO.TotalPetStat;

@Builder
public record GamificationResponse(
        MemberLevel memberLevel,
        List<PetLevel> petLevels
) {
    public static GamificationResponse of(TotalMemberTracking memberStat, List<TotalPetStat> petStats) {
        List<PetLevel> petLevels = petStats.stream()
                .map(PetLevel::from)
                .toList();

        return GamificationResponse.builder()
                .memberLevel(MemberLevel.from(memberStat))
                .petLevels(petLevels)
                .build();
    }

    @Builder
    public record MemberLevel(
            UUID memberId,
            Integer totalDistance,
            Integer totalDuration,
            Long totalCount,
            LevelInfo levelInfo
    ) {
        public static MemberLevel from(TotalMemberTracking stat) {
            return MemberLevel.builder()
                    .memberId(stat.memberId())
                    .totalDistance(stat.totalDistance())
                    .totalDuration(stat.totalDuration())
                    .totalCount(stat.totalCount())
                    .levelInfo(LevelInfo.of(stat.totalDistance(), stat.totalDuration()))
                    .build();
        }
    }

    @Builder
    public record PetLevel(
            UUID petId,
            String name,
            String profileImageUrl,
            String themeColor,
            Integer totalDistance,
            Integer totalDuration,
            Long totalCount,
            String badgeCode,
            LevelInfo levelInfo
    ) {
        public static PetLevel from(TotalPetStat stat) {
            PetBadge badge = PetBadge.getBadgeByDistance(stat.totalDistance());

            return PetLevel.builder()
                    .petId(stat.petId())
                    .name(stat.name())
                    .profileImageUrl(stat.profileImageUrl())
                    .themeColor(stat.themeColor())
                    .totalDistance(stat.totalDistance())
                    .totalDuration(stat.totalDuration())
                    .totalCount(stat.totalCount())
                    .badgeCode(badge.getCode())
                    .levelInfo(LevelInfo.of(stat.totalDistance(), stat.totalDuration()))
                    .build();
        }
    }
}
