package org.zerock.puppyrun.member.service;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.data.domain.PageRequest;
import org.zerock.puppyrun.a_config.TestContainerConfig;
import org.zerock.puppyrun.member.entity.Member;
import org.zerock.puppyrun.member.repository.MemberRepository;
import org.zerock.puppyrun.pet.entity.Breed;
import org.zerock.puppyrun.pet.entity.Pet;
import org.zerock.puppyrun.pet.repository.PetRepository;
import org.zerock.puppyrun.notification.entity.NotificationSettings;
import org.zerock.puppyrun.notification.entity.NotificationType;
import org.zerock.puppyrun.notification.repository.NotificationRepository;
import org.zerock.puppyrun.tracking.entity.Tracking;
import org.zerock.puppyrun.tracking.entity.TrackingImage;
import org.zerock.puppyrun.tracking.entity.Visibility;
import org.zerock.puppyrun.tracking.repository.TrackingImageRepository;
import org.zerock.puppyrun.tracking.repository.TrackingRepository;

class MemberServiceTest extends TestContainerConfig {

    @Autowired
    private MemberService memberService;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private PetRepository petRepository;

    @Autowired
    private TrackingRepository trackingRepository;

    @Autowired
    private TrackingImageRepository trackingImageRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("회원 탈퇴 시 펫과 산책 이미지는 삭제하고 산책은 작성자 없이 보존한다")
    void deleteAccount() {
        // given
        String rawPassword = "PuppyRun123!";
        Member member = memberRepository.save(Member.builder()
                .id(UUID.randomUUID())
                .nickName("탈퇴대상")
                .email("withdraw@example.com")
                .password(passwordEncoder.encode(rawPassword))
                .build());
        Pet pet = petRepository.save(Pet.builder()
                .member(member)
                .name("멍멍이")
                .breed(Breed.MALTESE)
                .isNeutered(true)
                .gender("M")
                .build());
        pet.updateProfile("test/pet-profile.png");
        Tracking tracking = trackingRepository.save(Tracking.builder()
                .member(member)
                .startedAt(LocalDateTime.of(2026, 1, 1, 10, 0))
                .endedAt(LocalDateTime.of(2026, 1, 1, 10, 30))
                .distance(1500)
                .visibility(Visibility.PUBLIC)
                .averagePace(5.0)
                .restDuration(0)
                .images(null)
                .build());
        trackingImageRepository.save(TrackingImage.builder()
                .tracking(tracking)
                .imageUrl("test/tracking-image.png")
                .imageOrder(0)
                .build());
        notificationRepository.save(NotificationSettings.builder()
                .member(member)
                .fcmToken("withdrawal-fcm-token")
                .isPushAgreed(true)
                .build());
        entityManager.flush();
        entityManager.clear();

        // when
        memberService.accountDelete(member.getId(), rawPassword);
        entityManager.flush();
        entityManager.clear();

        // then
        assertThat(memberRepository.findById(member.getId())).isEmpty();
        assertThat(petRepository.findById(pet.getId())).isEmpty();
        assertThat(trackingImageRepository.count()).isZero();
        assertThat(notificationRepository.findByMemberId(member.getId())).isEmpty();
        assertThat(notificationRepository.findNextMembers(
                null,
                null,
                PageRequest.of(0, 100),
                NotificationType.DAILY_WALKING_REMINDER
        ).content()).isEmpty();
        assertThat(trackingRepository.findById(tracking.getId())).isPresent()
                .get()
                .extracting(savedTracking -> savedTracking.getMember())
                .isNull();
    }
}
