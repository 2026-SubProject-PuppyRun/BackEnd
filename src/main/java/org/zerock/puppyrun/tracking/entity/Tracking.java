package org.zerock.puppyrun.tracking.entity;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
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
import org.zerock.puppyrun.tracking.DTO.UpdateTrackingDTO;

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

    @Column(nullable = false)
    private String averagePace;      // 평균 속도

    @Column(nullable = false)
    private Integer restDuration;    // 쉬는 시간

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Visibility visibility;

    // 이미지 리스트 매핑
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "tracking_images", joinColumns = @JoinColumn(name = "tracking_id"))
    @Column(name = "image_url")
    private List<String> images;

    @Convert(converter = TrackingPathConverter.class)
    @Column(name = "path", columnDefinition = "LONGTEXT")
    private List<TrackingPath> path;

    // TODO: 산책 루트를 이용하여 유사도 계산후 저장


    @Builder
    public Tracking(Member member, LocalDateTime startedAt, LocalDateTime endedAt, Integer distance,
                    Double startedLat, Double startedLng, Visibility visibility, List<TrackingPath> path,
                    String averagePace, List<String> images, Integer restDuration) {
        this.member = member;
        this.startedAt = startedAt;
        this.endedAt = endedAt;
        this.startedLat = startedLat;
        this.startedLng = startedLng;
        this.averagePace = averagePace;
        this.duration = (int) Duration.between(startedAt, endedAt).getSeconds();
        this.distance = distance;
        this.visibility = visibility;
        this.restDuration = restDuration;
        this.images = images != null ? images : List.of(); // TODO: 추후 S3 만들어지면 저장할 것
        this.path = path;
    }

    public void update(UpdateTrackingDTO updateTrackingDTO) {
        this.startedAt = updateTrackingDTO.startedAt();
        this.endedAt = updateTrackingDTO.endedAt();
        this.visibility = updateTrackingDTO.visibility();

        // 시간 변경에 따른 duration 재계산
        this.duration = (int) Duration.between(startedAt, endedAt).getSeconds();

    }

    public void changeVisibility(Visibility visibility) {
        this.visibility = visibility;
    }


    public boolean isNotOwner(UUID memberId) {
        return !this.member.getId().equals(memberId);
    }
}
