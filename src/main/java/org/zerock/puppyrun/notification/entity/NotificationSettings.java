package org.zerock.puppyrun.notification.entity;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.zerock.puppyrun.common.entity.BaseEntity;
import org.zerock.puppyrun.member.entity.Member;


@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NotificationSettings extends BaseEntity {
    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false, unique = true)
    private Member member;

    // 푸시 알림을 보낼 기기의 고유 주소
    @Column(name = "fcm_token", nullable = false)
    private String fcmToken;

    // 알림 수신 동의 여부 (푸시 알림은 법적으로 동의가 필수)
    @Column(name = "is_push_agreed", nullable = false)
    private boolean isPushAgreed;

    // 토큰의 활성 상태 (전송 실패 시 비활성화됨)
    @Column(name = "is_active", nullable = false)
    private boolean isActive;

    // 유저가 수신 거부(OFF)한 알림 타입만 모아두는 리스트
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "notification_opt_outs",
            joinColumns = @JoinColumn(name = "settings_id")
    )
    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type", length = 50)
    private Set<NotificationType> optOutTypes;


    @Builder
    public NotificationSettings(UUID id, Member member, String fcmToken, boolean isPushAgreed) {
        this.id = id != null ? id : UUID.randomUUID();
        this.member = member;
        this.fcmToken = fcmToken;
        this.isPushAgreed = isPushAgreed; // 할당
        this.isActive = isPushAgreed; // 수신 동의 시 자동으로 활성화
        this.optOutTypes = new HashSet<>();
    }


    public void updateToken(String fcmToken) {
        this.fcmToken = fcmToken;
        this.isActive = true; // 재등록 시 활성화
    }

    public void updatePushAgreed(boolean isPushAgreed) {
        this.isPushAgreed = isPushAgreed;
    }

    // 알림 차단(OFF)
    public void disableType(NotificationType type) {
        this.optOutTypes.add(type);
    }

    // 알림 다시 켜기(ON - 차단 목록에서 제거)
    public void enableType(NotificationType type) {
        this.optOutTypes.remove(type);
    }

    public Set<NotificationType> getAllowedTypes() {
        return Arrays.stream(NotificationType.values())
                .filter(type -> !optOutTypes.contains(type))
                .collect(Collectors.toUnmodifiableSet());
    }

}
