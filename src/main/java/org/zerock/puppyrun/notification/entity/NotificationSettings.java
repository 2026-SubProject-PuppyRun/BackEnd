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
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.zerock.puppyrun.common.entity.BaseTimeEntity;
import org.zerock.puppyrun.member.entity.Member;


@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NotificationSettings extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    // 푸시 알림을 보낼 기기의 고유 주소
    @Column(name = "fcm_token")
    private String fcmToken;

    // 알림 수신 동의 여부 (푸시 알림은 법적으로 동의가 필수)
    @Column(name = "is_push_agreed")
    private boolean isPushAgreed;


    // 유저가 수신 거부(OFF)한 알림 타입만 모아두는 리스트
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "notification_opt_outs",
            joinColumns = @JoinColumn(name = "settings_id")
    )
    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type")
    private Set<NotificationType> optOutTypes = new HashSet<>();


    @Builder
    public NotificationSettings(Member member, String fcmToken, boolean isPushAgreed) {
        this.member = member;
        this.fcmToken = fcmToken;
        this.isPushAgreed = isPushAgreed; // 할당
    }


    public void Update(String fcmToken, boolean isPushAgreed) {
        this.fcmToken = fcmToken;
        this.isPushAgreed = isPushAgreed; // 할당
    }

    // 알림 차단(OFF)
    public void disableType(NotificationType type) {
        this.optOutTypes.add(type);
    }

    // 알림 다시 켜기(ON - 차단 목록에서 제거)
    public void enableType(NotificationType type) {
        this.optOutTypes.remove(type);
    }

}
