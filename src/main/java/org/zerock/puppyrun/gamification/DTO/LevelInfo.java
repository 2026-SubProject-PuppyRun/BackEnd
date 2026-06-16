package org.zerock.puppyrun.gamification.DTO;

import lombok.Builder;
import org.zerock.puppyrun.gamification.util.GamificationLevelPolicy;

@Builder
public record LevelInfo(
        int level,
        int experience,
        int currentLevelExperience,
        int nextLevelExperience,
        int remainingExperience,
        Double progressPercent
) {
    public static LevelInfo of(int totalDistance, int totalDuration) {
        int experience = GamificationLevelPolicy.calculateExperience(totalDistance, totalDuration);
        int level = GamificationLevelPolicy.calculateLevel(experience);
        int currentLevelExperience = GamificationLevelPolicy.requiredExperienceForLevel(level);
        int nextLevelExperience = GamificationLevelPolicy.requiredExperienceForLevel(level + 1);
        int remainingExperience = Math.max(nextLevelExperience - experience, 0);
        double progressPercent = calculateProgressPercent(experience, currentLevelExperience, nextLevelExperience);

        return LevelInfo.builder()
                .level(level)
                .experience(experience)
                .currentLevelExperience(currentLevelExperience)
                .nextLevelExperience(nextLevelExperience)
                .remainingExperience(remainingExperience)
                .progressPercent(progressPercent)
                .build();
    }

    private static double calculateProgressPercent(int experience, int currentLevelExperience, int nextLevelExperience) {
        int requiredExperience = nextLevelExperience - currentLevelExperience;

        if (requiredExperience <= 0) {
            return 100.0;
        }

        double progress = (experience - currentLevelExperience) * 100.0 / requiredExperience;
        return Math.round(progress * 10) / 10.0;
    }
}
