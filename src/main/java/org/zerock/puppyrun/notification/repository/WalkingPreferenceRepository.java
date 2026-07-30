package org.zerock.puppyrun.notification.repository;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.zerock.puppyrun.notification.entity.WalkingPreference;

/**
 * 회원별 산책 선호도 영속화를 담당합니다.
 */
public interface WalkingPreferenceRepository extends JpaRepository<WalkingPreference, UUID> {

    Optional<WalkingPreference> findByMemberId(UUID memberId);
}
