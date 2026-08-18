package org.zerock.puppyrun.notification.repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.zerock.puppyrun.notification.entity.WalkingPreference;

/**
 * 산책 선호도 스냅샷의 조회 조건을 담당합니다.
 */
public interface WalkingPreferenceRepoCustom {

    /**
     * 지정한 회원의 기준 시각 범위 내 산책 선호도 스냅샷을 조회합니다.
     */
    List<WalkingPreference> findByMemberIdsAndCreatedAtBetween(
            Collection<UUID> memberIds,
            LocalDateTime from,
            LocalDateTime to
    );
}
