package org.zerock.puppyrun.notification.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.zerock.puppyrun.common.entity.BaseEntity;
import org.zerock.puppyrun.member.entity.Member;

/**
 * 특정 분석일의 회원 산책 선호 시간 결과를 저장합니다.
 *
 * <p>생성 시각은 결과의 24시간 유효성 판단에 사용하고, 분석일은 같은 회원의 같은 날
 * 스케줄 재실행으로 인한 중복 적재를 방지합니다.</p>
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(uniqueConstraints = @UniqueConstraint(
        name = "uk_walking_preference_member_analysis_date",
        columnNames = {"member_id", "analysis_date"}
))
public class WalkingPreference extends BaseEntity {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(name = "analysis_date", nullable = false)
    private LocalDate analysisDate;

    @Column(name = "last_known_latitude")
    private Double lastKnownLatitude;

    @Column(name = "last_known_longitude")
    private Double lastKnownLongitude;

    @Column(name = "last_known_date")
    private LocalDate lastKnownDate;

    @Column(name = "weekday_time")
    private LocalTime weekdayTime;

    @Column(name = "weekday_score")
    private Integer weekdayScore;

    @Column(name = "weekend_time")
    private LocalTime weekendTime;

    @Column(name = "weekend_score")
    private Integer weekendScore;

    @Builder
    public WalkingPreference(
            UUID id,
            Member member,
            LocalDate analysisDate,
            Double lastKnownLatitude,
            Double lastKnownLongitude,
            LocalDate lastKnownDate,
            LocalTime weekdayTime,
            Integer weekdayScore,
            LocalTime weekendTime,
            Integer weekendScore
    ) {
        this.id = id != null ? id : UUID.randomUUID();
        this.member = member;
        this.analysisDate = analysisDate;
        this.lastKnownLatitude = lastKnownLatitude;
        this.lastKnownLongitude = lastKnownLongitude;
        this.lastKnownDate = lastKnownDate;
        this.weekdayTime = weekdayTime;
        this.weekdayScore = weekdayScore;
        this.weekendTime = weekendTime;
        this.weekendScore = weekendScore;
    }

    /**
     * 마지막으로 확인된 산책 위치를 갱신합니다.
     *
     * @param latitude  최근 산책 위치의 위도
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
    public void updateTimePreferences(
            LocalTime weekdayTime,
            Integer weekdayScore,
            LocalTime weekendTime,
            Integer weekendScore
    ) {
        this.weekdayTime = weekdayTime;
        this.weekdayScore = weekdayScore;
        this.weekendTime = weekendTime;
        this.weekendScore = weekendScore;
    }
}
