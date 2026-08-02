package org.zerock.puppyrun.tracking.entity;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.zerock.puppyrun.a_config.TestContainerConfig;
import org.zerock.puppyrun.member.entity.Member;
import org.zerock.puppyrun.tracking.repository.TrackingRepository;

class TrackingImageRepositoryTest extends TestContainerConfig {

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private TrackingRepository trackingRepository;

    @Test
    @DisplayName("산책 기록을 저장하면 이미지 URL과 순서가 함께 저장된다")
    void saveTrackingImages() {
        // given
        Member member = persistMember("tracking-image-cascade");
        Tracking tracking = createTracking(member, List.of("first.jpg", "second.jpg", "third.jpg"));

        // when
        trackingRepository.save(tracking);
        entityManager.flush();
        entityManager.clear();

        // then
        Tracking savedTracking = trackingRepository.findById(tracking.getId()).orElseThrow();

        assertThat(savedTracking.getImages())
                .containsExactly("first.jpg", "second.jpg", "third.jpg");
        assertThat(savedTracking.getTrackingImages())
                .extracting(TrackingImage::getImageOrder)
                .containsExactly(0, 1, 2);
    }

    @Test
    @DisplayName("산책 이미지는 저장 순서와 관계없이 이미지 순번 오름차순으로 조회된다")
    void loadTrackingImagesByImageOrder() {
        // given
        Member member = persistMember("tracking-image-order");
        Tracking tracking = trackingRepository.save(createTracking(member, List.of()));
        entityManager.flush();

        // 연관관계 정렬 자체를 검증하기 위해 이미지를 순번과 다른 순서로 직접 저장합니다.
        entityManager.persist(createTrackingImage(tracking, "third.jpg", 2));
        entityManager.persist(createTrackingImage(tracking, "first.jpg", 0));
        entityManager.persist(createTrackingImage(tracking, "second.jpg", 1));
        entityManager.flush();
        entityManager.clear();

        // when
        Tracking savedTracking = trackingRepository.findById(tracking.getId()).orElseThrow();

        // then
        assertThat(savedTracking.getTrackingImages())
                .extracting(TrackingImage::getImageUrl)
                .containsExactly("first.jpg", "second.jpg", "third.jpg");
        assertThat(savedTracking.getFeaturedImage()).isEqualTo("first.jpg");
    }

    private Member persistMember(String identifier) {
        Member member = Member.builder()
                .email(identifier + "@test.com")
                .nickName(identifier)
                .password("encoded-password")
                .build();
        entityManager.persist(member);
        return member;
    }

    private Tracking createTracking(Member member, List<String> images) {
        return Tracking.builder()
                .member(member)
                .startedAt(LocalDateTime.of(2026, 8, 1, 10, 0))
                .endedAt(LocalDateTime.of(2026, 8, 1, 10, 30))
                .distance(2_000)
                .averagePace(6.5)
                .restDuration(300)
                .visibility(Visibility.PRIVATE)
                .images(images)
                .build();
    }

    private TrackingImage createTrackingImage(Tracking tracking, String imageUrl, int imageOrder) {
        return TrackingImage.builder()
                .tracking(tracking)
                .imageUrl(imageUrl)
                .imageOrder(imageOrder)
                .build();
    }
}
