package org.zerock.puppyrun.gamification.util;

public final class GamificationLevelPolicy {
    private static final int BASE_REQUIRED_EXPERIENCE = 10_000;
    private static final int DURATION_EXPERIENCE_PER_MINUTE = 10;
    private static final int SECONDS_TO_MINUTES = 60;

    private GamificationLevelPolicy() {
    }

    public static int calculateExperience(int totalDistance, int totalDuration) {
        long distanceExperience = Math.max(totalDistance, 0);
        long durationExperience = (Math.max(totalDuration, 0) / (long) SECONDS_TO_MINUTES)
                * DURATION_EXPERIENCE_PER_MINUTE;
        long experience = distanceExperience + durationExperience;

        return experience > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) experience;
    }

    public static int calculateLevel(int experience) {
        int safeExperience = Math.max(experience, 0);
        int level = 1;

        int nextRequiredExperience = requiredExperienceForLevel(level + 1);
        while (nextRequiredExperience < Integer.MAX_VALUE && safeExperience >= nextRequiredExperience) {
            level++;
            nextRequiredExperience = requiredExperienceForLevel(level + 1);
        }

        return level;
    }

    public static int requiredExperienceForLevel(int level) {
        if (level <= 1) {
            return 0;
        }

        long requiredExperience = (long) BASE_REQUIRED_EXPERIENCE * (level - 1) * level / 2;
        return requiredExperience > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) requiredExperience;
    }
}
