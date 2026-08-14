package org.zerock.puppyrun.notification.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import org.zerock.puppyrun.notification.entity.NotificationSettings;

public interface NotificationRepository extends JpaRepository<NotificationSettings, UUID>, NotificationRepoCustom {

    Optional<NotificationSettings> findByMemberId(UUID memberId);

    /**
     * 현재 활성 상태인 FCM 토큰을 한 번의 업데이트 쿼리로 비활성화합니다.
     *
     * <p>이미 비활성화됐거나 데이터베이스에 존재하지 않는 토큰은 변경 건수에 포함하지
     * 않습니다. 반환값을 이용해 요청 수와 실제 처리 수의 차이를 판단할 수 있습니다.</p>
     *
     * @param tokens 비활성화할 고유 FCM 토큰 목록
     * @return 실제로 활성 상태에서 비활성 상태로 변경된 행 수
     */
    @Transactional
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE NotificationSettings n
               SET n.isActive = false
             WHERE n.isActive = true
               AND n.fcmToken IN :tokens
            """)
    int deactivateActiveTokensByFcmToken(@Param("tokens") List<String> tokens);
}
