package org.zerock.puppyrun.tracking.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.zerock.puppyrun.common.entity.BaseTimeEntity;

import org.zerock.puppyrun.member.entity.Member;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "tracking")
public class Tracking extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY) // 다대일 관계, 지연 로딩
    @JoinColumn(name = "member_id", nullable = false) // 외래 키 컬럼명 지정
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Member member;

    @Column(nullable = false)
    private LocalDateTime startedAt; // 산책 시작 시간

    @Column(nullable = false)
    private LocalDateTime endedAt;   // 산책 종료 시간

    @Column(nullable = false)
    private Double startedLat;       // 시작 위치

    @Column(nullable = false)
    private Double startedLng;       // 시작 위치

    @Column(nullable = false)
    private Integer duration;        // 산책 진행 시간

    @Column(nullable = false)
    private Integer distance;        // 산책 거리


    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Visibility visibility;

    @Convert(converter = TrackingPathConverter.class)
    @Column(name = "path", columnDefinition = "LONGTEXT")
    private List<TrackingPath> path;

    // TODO: 산책 루트를 이용하여 유사도 계산후 저장


    @Builder
    public Tracking(Member member, LocalDateTime startedAt, LocalDateTime endedAt, Integer distance,
                    Double startedLat, Double startedLng, Visibility visibility, List<TrackingPath> path) {
        this.member = member;
        this.startedAt = startedAt;
        this.endedAt = endedAt;
        this.startedLat = startedLat;
        this.startedLng = startedLng;
        this.duration = (int) Duration.between(startedAt, endedAt).getSeconds();
        this.distance = distance;
        this.visibility = visibility;
        this.path = path;
    }


    public boolean isOwner(UUID memberId) {
        return this.member.getId().equals(memberId);
    }

}
