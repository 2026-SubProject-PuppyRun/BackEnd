package org.zerock.puppyrun.notification.entity;

import java.util.Arrays;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.zerock.puppyrun.common.exception.DataIntegrityException;
import org.zerock.puppyrun.pet.entity.PetBadge;

@Getter
@RequiredArgsConstructor
public enum NotificationType {
    // 시스템/공지 (SYS)
    NOTICE("SYS_001", Priority.HIGH, "전체 공지사항"),

    // 활동/산책 (ACT)
    DAILY_WALKING_REMINDER("ACT_001", Priority.HIGH, "일일 산책 리마인더"),
    WALK_GOAL_ACHIEVED("ACT_002", Priority.NORMAL, "주간 산책 목표 달성"),

    //  마케팅/이벤트 (MKT)
    EVENT_PROMO("MKT_001", Priority.NORMAL, "이벤트 및 프로모션 안내");


    private final String code;          // 알림 코드
    private final Priority priority;      // 알림 중요도
    private final String description;     // 설명

    public enum Priority {
        HIGH, NORMAL
    }

    /**
     * 문자열 코드값을 입력받아 해당하는 NotificationType Enum 상수를 반환합니다.
     *
     * @param code 뱃지 코드 (e.g., "SYS_001", "ACT_001")
     * @return 코드에 해당하는 NotificationType 상수
     * @throws DataIntegrityException 정의되지 않은 코드일 경우 예외 발생
     */
    public static NotificationType fromCode(String code) {
        return Arrays.stream(values())
                .filter(notificationType -> notificationType.code.equals(code))
                .findFirst()
                .orElseThrow(() -> new DataIntegrityException("잘못된 알림 코드입니다: " + code));
    }
}
