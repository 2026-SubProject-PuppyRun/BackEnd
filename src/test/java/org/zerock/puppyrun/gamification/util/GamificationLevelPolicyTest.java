package org.zerock.puppyrun.gamification.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.zerock.puppyrun.gamification.DTO.LevelInfo;

class GamificationLevelPolicyTest {

    @Test
    @DisplayName("레벨은 누적 경험치 구간에 따라 계산된다")
    void calculateLevel() {
        assertThat(GamificationLevelPolicy.calculateLevel(0)).isEqualTo(1);
        assertThat(GamificationLevelPolicy.calculateLevel(9_999)).isEqualTo(1);
        assertThat(GamificationLevelPolicy.calculateLevel(10_000)).isEqualTo(2);
        assertThat(GamificationLevelPolicy.calculateLevel(29_999)).isEqualTo(2);
        assertThat(GamificationLevelPolicy.calculateLevel(30_000)).isEqualTo(3);
    }

    @Test
    @DisplayName("경험치는 누적 거리와 산책 시간을 함께 반영한다")
    void calculateExperience() {
        int experience = GamificationLevelPolicy.calculateExperience(15_000, 3_000);

        assertThat(experience).isEqualTo(15_500);
    }

    @Test
    @DisplayName("레벨 정보는 다음 레벨까지 남은 경험치와 진행률을 제공한다")
    void createLevelInfo() {
        LevelInfo levelInfo = LevelInfo.of(15_000, 3_000);

        assertThat(levelInfo.level()).isEqualTo(2);
        assertThat(levelInfo.experience()).isEqualTo(15_500);
        assertThat(levelInfo.currentLevelExperience()).isEqualTo(10_000);
        assertThat(levelInfo.nextLevelExperience()).isEqualTo(30_000);
        assertThat(levelInfo.remainingExperience()).isEqualTo(14_500);
        assertThat(levelInfo.progressPercent()).isEqualTo(27.5);
    }
}
