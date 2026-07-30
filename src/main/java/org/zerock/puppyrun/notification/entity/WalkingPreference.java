package org.zerock.puppyrun.notification.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
import org.zerock.puppyrun.weather.DTO.RegionType;

/**
 * 회원의 최근 산책 지역과 평일·주말 선호 산책 시간을 저장합니다.
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

    @Enumerated(EnumType.STRING)
    @Column(name = "last_known_region")
    private RegionType lastKnownRegion;

    @Column(name = "preferred_weekday_time")
    private Integer preferredWeekdayTime;

    @Column(name = "preferred_weekend_time")
    private Integer preferredWeekendTime;

    @Builder
    public WalkingPreference(
            Member member,
            RegionType lastKnownRegion,
            Integer preferredWeekdayTime,
            Integer preferredWeekendTime
    ) {
        this.member = member;
        this.lastKnownRegion = lastKnownRegion;
        this.preferredWeekdayTime = preferredWeekdayTime;
        this.preferredWeekendTime = preferredWeekendTime;
    }

    /**
     * 마지막으로 확인된 산책 지역을 갱신합니다.
     *
     * @param region 최근 산책 지역
     */
    public void updateRegion(RegionType region) {
        this.lastKnownRegion = region;
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
