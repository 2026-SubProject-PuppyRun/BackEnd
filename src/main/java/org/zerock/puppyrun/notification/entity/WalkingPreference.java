package org.zerock.puppyrun.notification.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.zerock.puppyrun.common.entity.BaseEntity;
import org.zerock.puppyrun.member.entity.Member;

/**
 * 회원의 최근 산책 위치와 평일·주말 선호 산책 시간을 저장합니다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WalkingPreference extends BaseEntity {

    @Id
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "member_id")
    private Member member;

    @Column(name = "last_known_latitude")
    private Double lastKnownLatitude;

    @Column(name = "last_known_longitude")
    private Double lastKnownLongitude;

    @Column(name = "preferred_weekday_time")
    private Integer preferredWeekdayTime;

    @Column(name = "preferred_weekend_time")
    private Integer preferredWeekendTime;

    @Builder
    public WalkingPreference(
            Member member,
            Double lastKnownLatitude,
            Double lastKnownLongitude,
            Integer preferredWeekdayTime,
            Integer preferredWeekendTime
    ) {
        this.member = member;
        this.lastKnownLatitude = lastKnownLatitude;
        this.lastKnownLongitude = lastKnownLongitude;
        this.preferredWeekdayTime = preferredWeekdayTime;
        this.preferredWeekendTime = preferredWeekendTime;
    }

    /**
     * 마지막으로 확인된 산책 위치를 갱신합니다.
     *
     * @param latitude 최근 산책 위치의 위도
     * @param longitude 최근 산책 위치의 경도
     */
    public void updateLocation(double latitude, double longitude) {
        this.lastKnownLatitude = latitude;
        this.lastKnownLongitude = longitude;
    }

    /**
     * 분석된 평일·주말 선호 산책 시간을 갱신합니다.
     *
     * @param weekdayTime 평일 선호 시간
     * @param weekendTime 주말 선호 시간
     */
    public void updateTimePreferences(Integer weekdayTime, Integer weekendTime) {
        this.preferredWeekdayTime = weekdayTime;
        this.preferredWeekendTime = weekendTime;
    }
}
