package org.zerock.puppyrun.notification.service;

import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.zerock.puppyrun.common.auth.security.UserPrincipal;
import org.zerock.puppyrun.member.exception.UserNotFoundException;
import org.zerock.puppyrun.notification.entity.NotificationSettings;
import org.zerock.puppyrun.notification.entity.NotificationType;
import org.zerock.puppyrun.notification.execption.FCMNotFoundException;
import org.zerock.puppyrun.notification.repository.NotificationRepository;
import org.zerock.puppyrun.notification.controller.response.NotificationOptionsResponse;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationQueryService {
    private final NotificationRepository notificationRepository;

    /**
     * 사용자의 현재 알림 설정 및 개별 알림 옵션 상태를 조회합니다.
     *
     * @param userPrincipal 현재 인증된 사용자의 정보
     * @return 전체 알림 동의 여부 및 카테고리별로 그룹화된 개별 알림 설정 상태 응답 객체
     */
    public NotificationOptionsResponse getOptions(UserPrincipal userPrincipal) {
        NotificationSettings setting = notificationRepository.findByMemberId(userPrincipal.id())
                .orElseThrow(() -> new UserNotFoundException("해당 유저의 알림 설정을 찾을 수 없습니다."));

        // 현재 유저가 개별적으로 허용해둔 알림 타입들만 추출
        Set<NotificationType> optOuts = setting.getOptOutTypes();

        return NotificationOptionsResponse.of(setting, optOuts);
    }
}
