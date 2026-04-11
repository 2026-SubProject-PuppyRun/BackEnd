package org.zerock.puppyrun.notification.client;


import com.google.api.core.ApiFuture;
import com.google.api.core.ApiFutureCallback;
import com.google.api.core.ApiFutures;
import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.SendResponse;
import com.google.firebase.messaging.TopicManagementResponse;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.zerock.puppyrun.common.exception.BusinessException;
import org.zerock.puppyrun.common.exception.ErrorCode;
import org.zerock.puppyrun.common.exception.InvalidValueException;
import org.zerock.puppyrun.notification.service.DTO.PushTask;
import org.zerock.puppyrun.notification.repository.NotificationRepository;

@Slf4j
@Service
public class NotificationEventClient {
    // 구글이 허용하는 한 번의 최대 전송량
    private static final int MAX_FCM_BATCH_SIZE = 500;

    private final Executor executor;
    private final NotificationRepository notificationRepository;

    public NotificationEventClient(@Qualifier("notificationTaskExecutor") Executor executor,
                                   NotificationRepository notificationRepository) {
        this.executor = executor;
        this.notificationRepository = notificationRepository;
    }

    /**
     * FCM 메시지를 비동기적으로 일괄 발송합니다. 이 메서드는 호출 즉시 반환되며, 실제 전송은 백그라운드 스레드에서 처리됩니다.
     *
     * @param pushTasks 내용과 토큰이 각각 세팅된 Message 리스트
     */
    public void sendMessagesInBulk(List<PushTask> pushTasks) {
        if (pushTasks == null || pushTasks.isEmpty()) {
            log.info("푸시 알림이 비어있습니다.");
            return;
        }

        int totalSize = pushTasks.size();
        log.info("총 {}건의 푸시 알림 비동기 발송을 시작합니다.", totalSize);

        // 500개씩 묶어서(Chunk) 구글 서버에 전송
        for (int i = 0; i < totalSize; i += MAX_FCM_BATCH_SIZE) {

            int endIndex = Math.min(i + MAX_FCM_BATCH_SIZE, totalSize);
            List<PushTask> chunkedTask = pushTasks.subList(i, endIndex);
            List<Message> messages = chunkedTask.stream().map(PushTask::message).toList();

            // 비동기 sendEachAsync 호출로 ApiFuture를 받음
            ApiFuture<BatchResponse> future = FirebaseMessaging.getInstance().sendEachAsync(messages);

            // ApiFuture에 콜백을 등록하여 비동기 결과 처리
            ApiFutures.addCallback(future, new ApiFutureCallback<BatchResponse>() {
                // 전송 성공 시 (개별 메시지 실패 포함)
                @Override
                public void onSuccess(BatchResponse response) {
                    log.info("알림 묶음 전송 성공 (성공: {}, 실패: {})",
                            response.getSuccessCount(), response.getFailureCount());

                    // 실패한 토큰이 있다면 후처리 로직 호출
                    if (response.getFailureCount() > 0) {
                        handleFailedTokens(chunkedTask, response);
                    }
                }

                //  전송 실패시
                @Override
                public void onFailure(Throwable t) {
                    log.error("알림 묶음 전송 중 오류 발생", t);
                }
            }, executor);


        }
    }

    /**
     * FCM 토픽 방식을 사용하여 메시지를 비동기적으로 일괄 발송
     *
     * @param pushTask 발송할 메시지
     */
    public void sendTopicMessage(PushTask pushTask) {
        if (pushTask == null) {
            log.info("푸시 알림이 비어있습니다.");
            return;
        }

        if (pushTask.topic() == null) {
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "토픽 정보가 비어있습니다.");
        }

        log.info("토픽 [{}] 대상 푸시 알림 비동기 발송을 시작합니다.", pushTask.topic());

        // 여러 개의 토큰 리스트 대신, 단 한 번의 호출로 구글 서버에 토픽 발송을 위임
        ApiFuture<String> future = FirebaseMessaging.getInstance().sendAsync(pushTask.message());

        // ApiFuture에 콜백을 등록하여 비동기 결과 처리
        ApiFutures.addCallback(future, new ApiFutureCallback<String>() {

            // 토픽 발송 요청 성공 시 (개별 유저 수신 여부와는 무관함)
            @Override
            public void onSuccess(String messageId) {
                log.info("토픽 [{}] 알림 전송 요청 성공! (MessageID: {})", pushTask.topic(), messageId);
            }

            // 토픽 발송 요청 자체가 실패했을 때 (네트워크 오류, 인증 오류 등)
            @Override
            public void onFailure(Throwable t) {
                log.error("토픽 [{}] 알림 전송 중 오류 발생", pushTask.topic(), t);
            }
        }, executor);
    }


    /**
     * 특정 FCM 토큰을 특정 토픽에 구독 또는 구독 취소합니다.
     *
     * @param fcmToken    대상 FCM 토큰
     * @param typeCode    대상 토픽 이름 (예: SYS_001)
     * @param isSubscribe 구독 여부 (true: 구독, false: 구독 취소)
     */
    public void manageTopicSubscription(String fcmToken, String typeCode, boolean isSubscribe) {
        ApiFuture<TopicManagementResponse> future;

        if (isSubscribe) {
            future = FirebaseMessaging.getInstance().subscribeToTopicAsync(
                    Collections.singletonList(fcmToken), typeCode
            );
            log.info("FCM 토픽[{}] 구독 요청 완료 (토큰: {})", typeCode, fcmToken);
        } else {
            future = FirebaseMessaging.getInstance().unsubscribeFromTopicAsync(
                    Collections.singletonList(fcmToken), typeCode
            );
            log.info("FCM 토픽[{}] 구독 취소 요청 완료 (토큰: {})", typeCode, fcmToken);
        }

        ApiFutures.addCallback(future, new ApiFutureCallback<TopicManagementResponse>() {
            @Override
            public void onSuccess(TopicManagementResponse result) {
                log.info("FCM 토픽[{}] 처리 완료", typeCode);
            }

            @Override
            public void onFailure(Throwable t) {
                log.error("FCM 토픽[{}] 처리 실패: {}", typeCode, t.getMessage());
            }
        }, executor);
    }


    /**
     * FCM 토큰 유효성 검증
     *
     * @param fcmToken 대상 FCM 토큰
     */
    public void validateFcmToken(String fcmToken) {
        try {
            Message message = Message.builder()
                    .setToken(fcmToken)
                    .build();

            // true로 설정하면 사용자에게 실제 알림이 가지 않고 유효성만 검사 진행
            FirebaseMessaging.getInstance().send(message, true);
            log.info("FCM 토큰 유효성 검증 통과");

        } catch (FirebaseMessagingException e) {
            log.warn("유효하지 않거나 만료된 FCM 토큰입니다. 이유: {}", e.getMessage());
            throw new InvalidValueException("유효하지 않은 FCM 토큰입니다.");
        } catch (IllegalArgumentException e) {
            log.warn("잘못된 형식의 FCM 토큰입니다. 이유: {}", e.getMessage());
            throw new InvalidValueException("잘못된 형식의 FCM 토큰입니다.");
        }
    }


    /**
     * 실패한 토큰들을 걸러내고 DB에서 비활성화 처리하는 후처리 메서드
     */
    private void handleFailedTokens(List<PushTask> sentPushTasks, BatchResponse batchResponse) {
        List<SendResponse> responses = batchResponse.getResponses();
        List<String> deadTokens = new ArrayList<>();

        for (int i = 0; i < responses.size(); i++) {
            if (!responses.get(i).isSuccessful()) {
                // 어떤 토큰이 실패했는지 찾아냄 (보낸 리스트와 응답 리스트의 순서가 일치)
                String failedToken = sentPushTasks.get(i).fcmToken();
                log.warn("발송 실패한 토큰 발견. 비활성화 대상: {}, 원인: {}", failedToken,
                        responses.get(i).getException().getMessage());
                deadTokens.add(failedToken);

            }
        }

        if (!deadTokens.isEmpty()) {
            log.info("{}개의 만료된 토큰을 데이터베이스에서 비활성화 처리합니다.", deadTokens.size());
            try {
                notificationRepository.deactivateTokensByFcmToken(deadTokens);
                log.info("토큰 비활성화 완료");
            } catch (Exception e) {
                log.error("토큰 비활성화 처리 중 오류 발생", e);
            }
        }
    }


}
