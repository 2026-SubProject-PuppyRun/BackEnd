package org.zerock.puppyrun.notification.service;

import java.util.Arrays;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.zerock.puppyrun.common.auth.security.UserPrincipal;
import org.zerock.puppyrun.common.exception.BusinessException;
import org.zerock.puppyrun.common.exception.ExistsResourceException;
import org.zerock.puppyrun.member.entity.Member;
import org.zerock.puppyrun.member.exception.UserNotFoundException;
import org.zerock.puppyrun.member.repository.MemberRepository;
import org.zerock.puppyrun.notification.client.NotificationEventClient;
import org.zerock.puppyrun.notification.controller.request.FcmTokenUpdateRequest;
import org.zerock.puppyrun.notification.controller.request.NotificationAgreeRequest;
import org.zerock.puppyrun.notification.controller.request.NotificationGlobalToggleRequest;
import org.zerock.puppyrun.notification.controller.request.NotificationToggleRequest;
import org.zerock.puppyrun.notification.entity.NotificationSettings;
import org.zerock.puppyrun.notification.entity.NotificationType;
import org.zerock.puppyrun.notification.execption.FCMNotFoundException;
import org.zerock.puppyrun.notification.repository.NotificationRepository;
import org.zerock.puppyrun.common.exception.InvalidValueException;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class NotificationCommandService {
    private final NotificationRepository notificationRepository;
    private final MemberRepository memberRepository;
    private final NotificationEventClient notificationEventClient;

    /**
     * 특정 알림 타입의 수신 여부를 개별적으로 켜거나 끕니다. 알림 상태 변경 시, 해당 알림에 매핑된 FCM 토픽 구독 상태도 함께 동기화됩니다.
     *
     * @param userPrincipal 현재 인증된 사용자의 정보
     * @param request       알림 활성화 여부 (true: 켜기, false: 끄기)
     * @throws InvalidValueException 전체 알림 수신 동의가 꺼져있거나, 잘못된 알림 코드일 때 발생
     */
    public void toggle(UserPrincipal userPrincipal, NotificationToggleRequest request) {
        NotificationSettings setting = findSetting(userPrincipal);
        String typeCode = request.optionCode();
        boolean isEnable = request.enabled();

        if (!setting.isPushAgreed()) {
            throw new InvalidValueException("알림 수신 동의가 필요합니다.");
        }

        // 알림 코드로 알림 타입 정의
        NotificationType type;
        try {
            type = NotificationType.fromCode(typeCode);
        } catch (BusinessException e) {
            throw new InvalidValueException("잘못된 알림 코드입니다: " + typeCode);
        }

        // 현재 유저가 개별적으로 미허용 해둔 알림 타입들만 추출
        Set<NotificationType> optOuts = setting.getOptOutTypes();

        if (isEnable && optOuts.contains(type)) {
            // 알림 켜기
            setting.enableType(type);
            manageTopicSubscription(setting.getFcmToken(), type, true);
        } else if (!isEnable && !optOuts.contains(type)) {
            // 알림 끄기
            setting.disableType(type);
            manageTopicSubscription(setting.getFcmToken(), type, false);
        } else {
            log.info("알림 상태가 바꾸려는 상태와 동일합니다.");
        }
    }

    /**
     * 사용자의 전체 알림 수신 동의 여부를 변경하고, 허용된 개별 알림들에 대한 FCM 토픽 구독 상태를 일괄 동기화합니다. 기존의 개별 알림 세부 설정(optOutTypes)은 유지됩니다.
     *
     * @param userPrincipal 현재 인증된 사용자의 정보
     * @param request       전체 알림 수신 동의 활성화 여부 (true: 전체 허용, false: 전체 차단)
     */
    public void toggleGlobal(UserPrincipal userPrincipal, NotificationGlobalToggleRequest request) {
        NotificationSettings setting = findSetting(userPrincipal);
        boolean isEnable = request.isPushAgreed();

        // 현재 상태와 목표 상태가 같으면 불필요한 쿼리 및 FCM API 호출 방지
        if (setting.isPushAgreed() == isEnable) {
            log.info("현재 알림 상태가 바꾸려는 상태와 동일합니다.");
            return;
        }

        // DB 상태 업데이트
        setting.updatePushAgreed(isEnable);

        // 현재 유저가 개별적으로 허용해둔 알림 타입들만 추출
        Set<NotificationType> allowedTypes = setting.getAllowedTypes();

        // 토픽 구독 상태 동기화
        allowedTypes.forEach(type ->
                manageTopicSubscription(setting.getFcmToken(), type, isEnable));

    }

    /**
     * 알림 설정 저장
     *
     * @param userPrincipal 현재 인증된 사용자의 정보
     * @param request       알림 수신 동의 요청
     */
    public void saveNotification(UserPrincipal userPrincipal, NotificationAgreeRequest request) {
        if (notificationRepository.findByMemberId(userPrincipal.id()).isPresent()) {
            throw new ExistsResourceException("이미 알림 설정이 존재합니다.");
        }
        // 토큰 유효성 검사 진행
        notificationEventClient.validateFcmToken(request.fcmToken());

        Member member = memberRepository.findByIdOrThrow(userPrincipal.id());

        NotificationSettings settings = NotificationSettings.builder()
                .fcmToken(request.fcmToken())
                .member(member)
                .isPushAgreed(request.isPushAgreed())
                .build();
        notificationRepository.save(settings);

        // 동의할 경우 모든 알림 on
        if (request.isPushAgreed()) {
            Arrays.stream(NotificationType.values())
                    .forEach(type -> manageTopicSubscription(request.fcmToken(), type, true));
        }

    }

    public void updateToken(UserPrincipal userPrincipal, FcmTokenUpdateRequest request) {
        NotificationSettings settings = notificationRepository.findByMemberId(userPrincipal.id())
                .orElseThrow(() -> new UserNotFoundException("해당 유저의 알림 설정을 찾을 수 없습니다."));

        // 토큰 유효성 검사 진행
        notificationEventClient.validateFcmToken(request.fcmToken());
        String currentToken = settings.getFcmToken();
        String newToken = request.fcmToken();
        // 값이 같고 이미 활성화 상태라면 아무것도 하지 않음
        if (newToken.equals(currentToken) && settings.isActive()) {
            log.info("기존과 동일한 토큰이며 활성 상태이므로 갱신을 생략합니다. (memberId: {})", userPrincipal.id());
            return;
        }
        // 기존 토큰과 다르거나, 현재 비활성화(발송 실패 등) 상태일 때만 업데이트 수행
        log.info("FCM 토큰을 갱신하고 활성화 상태로 변경합니다. (memberId: {})", userPrincipal.id());
        settings.updateToken(newToken);
        Set<NotificationType> allowedTypes = settings.getAllowedTypes();
        // 기존 토큰 토픽 제거
        allowedTypes.forEach(type -> manageTopicSubscription(currentToken, type, false));
        // 토픽 재구독 처리
        if (settings.isPushAgreed()) {
            allowedTypes.forEach(type -> manageTopicSubscription(newToken, type, true));
        }
    }

    /**
     * 인증된 사용자 정보를 기반으로 NotificationSettings 엔티티를 조회하는 내부 헬퍼 메서드입니다.
     *
     * @param userPrincipal 현재 인증된 사용자의 정보
     * @return 해당 유저의 알림 설정 엔티티
     * @throws UserNotFoundException 알림 설정을 찾을 수 없을 때 발생
     * @throws FCMNotFoundException  FCM 토큰이 비어있을 때 발생
     */
    private NotificationSettings findSetting(UserPrincipal userPrincipal) {
        NotificationSettings settings = notificationRepository.findByMemberId(userPrincipal.id())
                .orElseThrow(() -> new UserNotFoundException("해당 유저의 알림 설정을 찾을 수 없습니다."));

        if (settings.getFcmToken().isBlank()) {
            throw new FCMNotFoundException("FCM 토큰이 비어있습니다.");
        }

        return settings;
    }

    /**
     * 구글 Firebase 서버에 특정 토픽의 구독 또는 구독 취소를 비동기적으로 요청합니다.
     *
     * @param fcmToken    사용자의 알림 FCM 토큰
     * @param type        구독 또는 취소할 대상 알림 타입
     * @param isSubscribe 구독 여부 (true: 구독 요청, false: 구독 취소 요청)
     */
    private void manageTopicSubscription(String fcmToken, NotificationType type, boolean isSubscribe) {
        notificationEventClient.manageTopicSubscription(fcmToken, type.getCode(), isSubscribe);
    }
}
