package org.zerock.puppyrun.diary.entity;

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
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
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
import org.zerock.puppyrun.diary.DTO.UpdateDiaryDTO;
import org.zerock.puppyrun.member.entity.Member;
import org.zerock.puppyrun.tracking.entity.Tracking;
import org.zerock.puppyrun.weather.DTO.PrecipitationType;
import org.zerock.puppyrun.weather.DTO.SkyType;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "diary")
public class Diary extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 100)
    private String title; // 일기 제목

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content; // 일기 내용

    @ManyToOne(fetch = FetchType.LAZY) // 다대일 관계, 지연 로딩
    @JoinColumn(name = "member_id", nullable = false) // 외래 키 컬럼명 지정
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Member member;

    // 날씨 정보
    @Column(name = "writing_time", nullable = false, length = 20)
    private LocalDateTime writingTime; // 일기 작성 시간대 정보

    @Column(nullable = false, length = 20)
    private String temp; // 기온

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SkyType sky; // 하늘

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PrecipitationType pty; // 강수


    // 연관된 산책 기록 (Tracking 엔티티와 일대일 관계)
    // 산책 기록이 삭제되어도 일기는 유지될 수 있도록 nullable입니다.
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tracking_id")
    private Tracking tracking;

    // 이미지 리스트 매핑
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "diary_images", joinColumns = @JoinColumn(name = "diary_id"))
    @Column(name = "image_url")
    private List<String> images;


    @Builder
    public Diary(String title, String content, Member member, String temp, SkyType sky, PrecipitationType pty,
                 LocalDateTime writingTime, Tracking tracking, List<String> images) {
        this.title = title;
        this.content = content;
        this.member = member;
        this.writingTime = writingTime;
        this.temp = temp;
        this.sky = sky;
        this.pty = pty;
        this.tracking = tracking;
        this.images = images != null ? images : List.of(); // TODO: 추후 S3 만들어지면 저장할 것
    }

    /**
     * 일기 정보를 수정합니다.
     * <p>이미지 리스트는 기존 리스트를 비우고 새로운 리스트로 교체됩니다.</p>
     *
     * @param updateDiaryDTO 수정할 정보가 담긴 DTO
     */
    public void update(UpdateDiaryDTO updateDiaryDTO) {
        this.title = updateDiaryDTO.title();
        this.content = updateDiaryDTO.content();
        this.writingTime = updateDiaryDTO.writingTime();
        this.temp = updateDiaryDTO.temp();
        this.sky = updateDiaryDTO.sky();
        this.pty = updateDiaryDTO.pty();

        if (updateDiaryDTO.images() != null) {
            this.images.clear();
            this.images.addAll(updateDiaryDTO.images());
        }
    }

    /**
     * 산책 기록과의 연관관계를 해제합니다.
     */
    public void unsetTracking() {
        this.tracking = null;
    }

    /**
     * 요청한 사용자가 일기의 작성자가 아닌지 확인합니다.
     *
     * @param memberId 요청한 사용자의 UUID
     * @return 작성자가 아니면 true, 작성자면 false
     */
    public boolean isNotOwner(UUID memberId) {
        return !this.member.getId().equals(memberId);
    }

}
