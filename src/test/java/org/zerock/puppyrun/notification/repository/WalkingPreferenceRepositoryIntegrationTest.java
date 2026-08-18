package org.zerock.puppyrun.notification.repository;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.zerock.puppyrun.a_config.TestContainerConfig;
import org.zerock.puppyrun.member.entity.Member;
import org.zerock.puppyrun.notification.entity.WalkingPreference;

class WalkingPreferenceRepositoryIntegrationTest extends TestContainerConfig {

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private WalkingPreferenceRepository walkingPreferenceRepository;

    @Test
    @DisplayName("회원과 분석일로 조회하면 해당 날짜의 산책 선호 시간 결과만 반환한다")
    void findAllByMemberIdInAndAnalysisDate() {
        // given
        Member member = Member.builder()
                .email("walking-preference@test.com")
                .nickName("walking-preference")
                .password("encoded-password")
                .build();
        entityManager.persist(member);
        LocalDate analysisDate = LocalDate.of(2026, 8, 14);
        walkingPreferenceRepository.saveAll(List.of(
                preference(member, analysisDate, LocalTime.of(18, 0), 10),
                preference(member, analysisDate.minusDays(1), LocalTime.of(8, 0), 4)
        ));
        entityManager.flush();
        entityManager.clear();

        // when
        List<WalkingPreference> preferences = walkingPreferenceRepository
                .findAllByMemberIdInAndAnalysisDate(List.of(member.getId()), analysisDate);

        // then
        assertThat(preferences)
                .singleElement()
                .satisfies(preference -> {
                    assertThat(preference.getAnalysisDate()).isEqualTo(analysisDate);
                    assertThat(preference.getWeekdayTime()).isEqualTo(LocalTime.of(18, 0));
                    assertThat(preference.getWeekdayScore()).isEqualTo(10);
                });
    }

    private WalkingPreference preference(
            Member member,
            LocalDate analysisDate,
            LocalTime weekdayTime,
            int weekdayScore
    ) {
        return WalkingPreference.builder()
                .id(UUID.randomUUID())
                .member(member)
                .analysisDate(analysisDate)
                .weekdayTime(weekdayTime)
                .weekdayScore(weekdayScore)
                .build();
    }
}
