package org.zerock.puppyrun.tracking.entity;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.zerock.puppyrun.common.entity.BaseEntity;
import org.zerock.puppyrun.member.entity.Member;
import org.zerock.puppyrun.tracking.DTO.UpdateTrackingDTO;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "tracking")
public class Tracking extends BaseEntity {
    @Id
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
    private Integer duration;        // 산책 진행 시간

    @Column(nullable = false)
    private Integer distance;        // 산책 거리

    @Column(name = "average_pace")
    private Double averagePace;      // 평균 속도

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

    @Builder
    public Tracking(UUID id, Member member, LocalDateTime startedAt, LocalDateTime endedAt, Integer distance,
                    Visibility visibility, Double averagePace, List<String> images, Integer restDuration) {
        this.id = id != null ? id : UUID.randomUUID();
        this.member = member;
        this.startedAt = startedAt;
        this.endedAt = endedAt;
        this.averagePace = averagePace;
        this.duration = (int) Duration.between(startedAt, endedAt).getSeconds();
        this.distance = distance;
        this.visibility = visibility;
        this.restDuration = restDuration;
        this.images = images != null ? new ArrayList<>(images) : new ArrayList<>();
    }

    public void update(UpdateTrackingDTO updateTrackingDTO) {
        this.startedAt = updateTrackingDTO.startedAt();
        this.endedAt = updateTrackingDTO.endedAt();
        this.visibility = updateTrackingDTO.visibility();

        // 시간 변경에 따른 duration 재계산
        this.duration = (int) Duration.between(startedAt, endedAt).getSeconds();

    }

    public void uploadImages(List<String> images) {
        this.images = images != null ? new ArrayList<>(images) : new ArrayList<>();
    }

    public void changeVisibility(Visibility visibility) {
        this.visibility = visibility;
    }


    public boolean isNotOwner(UUID memberId) {
        return !this.member.getId().equals(memberId);
    }
}
