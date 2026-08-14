package org.zerock.puppyrun.notification.client;


import com.google.api.core.ApiFuture;
import com.google.api.core.ApiFutureCallback;
import com.google.api.core.ApiFutures;
import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.SendResponse;
import com.google.firebase.messaging.TopicManagementResponse;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.zerock.puppyrun.common.exception.InvalidValueException;
import org.zerock.puppyrun.notification.repository.NotificationRepository;
import org.zerock.puppyrun.notification.client.DTO.PushTask;

/**
 * Firebase Cloud Messaging과 통신해 푸시 발송, 토픽 구독 및 토큰 검증을 수행합니다.
 *
 * <p>발송 결과는 별도 실행기에서 비동기 처리합니다. 개별 전송 실패는 FCM 오류 코드별로
 * 기록하며, 더 이상 유효하지 않음이 확정된 {@link MessagingErrorCode#UNREGISTERED} 토큰만 데이터베이스에서 비활성화합니다.</p>
 */
@Slf4j
@Service
public class NotificationEventClient {
    // 구글이 허용하는 한 번의 최대 전송량
    private static final int MAX_FCM_BATCH_SIZE = 500;

    private final Executor executor;
    private final NotificationRepository notificationRepository;

    public NotificationEventClient(
            @Qualifier("notificationTaskExecutor") Executor executor,
            NotificationRepository notificationRepository
    ) {
        this.executor = executor;
        this.notificationRepository = notificationRepository;
    }

    /**
     * FCM 메시지를 비동기적으로 일괄 발송합니다. 이 메서드는 호출 즉시 반환되며, 실제 전송은 백그라운드 스레드에서 처리됩니다.
     *
     * @param pushTasks FCM 토큰 대상 메시지 생성 계약 목록
     */
    public void sendMessagesInBulk(List<PushTask> pushTasks) {
        if (pushTasks == null) {
            log.warn("event=notification_token_batch_skipped, reason=null_tasks");
            return;
        }
        if (pushTasks.isEmpty()) {
            log.debug("event=notification_token_batch_skipped, reason=empty_tasks");
            return;
        }

        int totalSize = pushTasks.size();
        log.info("event=notification_token_batch_started, totalCount={}", totalSize);

        // 500개씩 묶어서(Chunk) 구글 서버에 전송
        for (int i = 0; i < totalSize; i += MAX_FCM_BATCH_SIZE) {

            int endIndex = Math.min(i + MAX_FCM_BATCH_SIZE, totalSize);
            List<PushTask> chunkedTask = pushTasks.subList(i, endIndex);
            List<Message> messages = chunkedTask.stream()
                    .map(PushTask::getMessage)
                    .toList();

            // 비동기 sendEachAsync 호출로 ApiFuture를 받음
            ApiFuture<BatchResponse> future = FirebaseMessaging.getInstance().sendEachAsync(messages);

            // ApiFuture에 콜백을 등록하여 비동기 결과 처리
            ApiFutures.addCallback(future, new ApiFutureCallback<BatchResponse>() {
                // 전송 성공 시 (개별 메시지 실패 포함)
                @Override
                public void onSuccess(BatchResponse response) {
                    if (response.getFailureCount() > 0) {
                        log.warn(
                                "event=notification_token_batch_completed, result=partial_failure, "
                                        + "requestedCount={}, successCount={}, failureCount={}",
                                chunkedTask.size(),
                                response.getSuccessCount(),
                                response.getFailureCount()
                        );
                        handleFailedTokens(chunkedTask, response);
                        return;
                    }

                    log.info(
                            "event=notification_token_batch_completed, result=success, "
                                    + "requestedCount={}, successCount={}, failureCount={}",
                            chunkedTask.size(),
                            response.getSuccessCount(),
                            response.getFailureCount()
                    );
                }

                //  전송 실패시
                @Override
                public void onFailure(Throwable t) {
                    log.error(
                            "event=notification_token_batch_completed, result=failed, "
                                    + "requestedCount={}, exceptionType={}",
                            chunkedTask.size(),
                            t.getClass().getSimpleName(),
                            t
                    );
                }
            }, executor);


        }
    }

    /**
     * PushTask가 지정한 전송 대상으로 메시지를 비동기 발송합니다.
     *
     * @param pushTask 발송할 메시지
     */
    public void sendMessage(PushTask pushTask) {
        if (pushTask == null) {
            log.warn("event=notification_message_send_skipped, reason=null_task");
            return;
        }

        log.info("event=notification_message_send_started, target={}", pushTask.target());

        // 단일 PushTask가 만든 대상 메시지를 FCM에 전달한다.
        Message message = pushTask.getMessage();
        ApiFuture<String> future = FirebaseMessaging.getInstance().sendAsync(message);

        // ApiFuture에 콜백을 등록하여 비동기 결과 처리
        ApiFutures.addCallback(future, new ApiFutureCallback<String>() {

            // FCM 발송 요청 성공 시 (개별 기기 수신 여부와는 무관함)
            @Override
            public void onSuccess(String messageId) {
                log.info(
                        "event=notification_message_send_completed, result=success, target={}, messageId={}",
                        pushTask.target(),
                        messageId
                );
            }

            // FCM 발송 요청 자체가 실패했을 때 (네트워크 오류, 인증 오류 등)
            @Override
            public void onFailure(Throwable t) {
                log.error(
                        "event=notification_message_send_completed, result=failed, target={}, exceptionType={}",
                        pushTask.target(),
                        t.getClass().getSimpleName(),
                        t
                );
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
        String action = isSubscribe ? "subscribe" : "unsubscribe";

        if (isSubscribe) {
            future = FirebaseMessaging.getInstance().subscribeToTopicAsync(
                    Collections.singletonList(fcmToken), typeCode
            );
        } else {
            future = FirebaseMessaging.getInstance().unsubscribeFromTopicAsync(
                    Collections.singletonList(fcmToken), typeCode
            );
        }
        log.debug("event=notification_topic_subscription_requested, action={}, topic={}", action, typeCode);

        ApiFutures.addCallback(future, new ApiFutureCallback<TopicManagementResponse>() {
            @Override
            public void onSuccess(TopicManagementResponse result) {
                if (result.getFailureCount() > 0) {
                    log.error(
                            "event=notification_topic_subscription_completed, result=failed, "
                                    + "action={}, topic={}, successCount={}, failureCount={}",
                            action,
                            typeCode,
                            result.getSuccessCount(),
                            result.getFailureCount()
                    );
                    return;
                }

                log.debug(
                        "event=notification_topic_subscription_completed, result=success, "
                                + "action={}, topic={}, successCount={}",
                        action,
                        typeCode,
                        result.getSuccessCount()
                );
            }

            @Override
            public void onFailure(Throwable t) {
                log.error(
                        "event=notification_topic_subscription_completed, result=failed, "
                                + "action={}, topic={}, exceptionType={}",
                        action,
                        typeCode,
                        t.getClass().getSimpleName(),
                        t
                );
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
            log.debug("event=notification_token_validation, result=success");

        } catch (FirebaseMessagingException e) {
            log.debug(
                    "event=notification_token_validation, result=invalid, exceptionType={}, reason={}",
                    e.getClass().getSimpleName(),
                    e.getMessage()
            );
            throw new InvalidValueException("유효하지 않은 FCM 토큰입니다.", e);
        } catch (IllegalArgumentException e) {
            log.debug(
                    "event=notification_token_validation, result=malformed, exceptionType={}, reason={}",
                    e.getClass().getSimpleName(),
                    e.getMessage()
            );
            throw new InvalidValueException("잘못된 형식의 FCM 토큰입니다.", e);
        }
    }


    /**
     * 배치 응답의 개별 실패를 FCM 오류 코드별로 분류하고 영구 만료 토큰을 비활성화합니다.
     *
     * <p>{@link BatchResponse#getResponses()}의 응답 순서가 입력 메시지 순서와 동일하다는
     * FCM 계약을 이용해 실패 응답과 토큰을 연결합니다. 중복 토큰은 한 번만 처리하며, {@link MessagingErrorCode#UNREGISTERED} 오류가 발생한 토큰만 일괄
     * 업데이트합니다.</p>
     *
     * @param sentPushTasks FCM에 전달한 토큰 전송 작업 목록
     * @param batchResponse 전송 작업별 성공·실패 결과
     */
    void handleFailedTokens(List<PushTask> sentPushTasks, BatchResponse batchResponse) {
        List<SendResponse> responses = batchResponse.getResponses();
        Set<String> tokensToDeactivate = new LinkedHashSet<>();

        for (int i = 0; i < responses.size(); i++) {
            SendResponse response = responses.get(i);
            if (response.isSuccessful()) {
                continue;
            }

            // BatchResponse의 응답 순서는 전송한 메시지 순서와 동일하다.
            String failedToken = sentPushTasks.get(i).target();
            FirebaseMessagingException exception = response.getException();
            if (failedToken != null && shouldDeactivateToken(failedToken, exception)) {
                tokensToDeactivate.add(failedToken);
            }
        }

        if (tokensToDeactivate.isEmpty()) {
            return;
        }

        List<String> tokens = List.copyOf(tokensToDeactivate);
        try {
            int deactivatedCount = notificationRepository.deactivateActiveTokensByFcmToken(tokens);
            logDeactivationResult(tokens.size(), deactivatedCount);
        } catch (Exception e) {
            log.error(
                    "event=notification_tokens_deactivated, result=failed, requestedCount={}, exceptionType={}",
                    tokens.size(),
                    e.getClass().getSimpleName(),
                    e
            );
        }
    }

    /**
     * FCM 오류 코드에 따른 운영 로그를 남기고 자동 비활성화 여부를 결정합니다.
     *
     * <p>{@code INVALID_ARGUMENT}는 잘못된 토큰뿐 아니라 메시지 페이로드 오류에도 발생할 수
     * 있으므로 자동 비활성화하지 않습니다. 할당량, 일시 장애, 내부 오류 및 Firebase 설정 오류도 토큰 자체의 만료를 의미하지 않으므로 유지합니다.</p>
     *
     * @param fcmToken  전송에 실패한 FCM 토큰
     * @param exception 개별 전송 실패에 포함된 Firebase 예외
     * @return 토큰이 영구 만료되어 자동 비활성화해야 하면 {@code true}
     */
    private boolean shouldDeactivateToken(String fcmToken, FirebaseMessagingException exception) {
        String maskedToken = maskFcmToken(fcmToken);
        if (exception == null || exception.getMessagingErrorCode() == null) {
            log.error(
                    "event=notification_token_send_failed, errorCode=UNKNOWN, action=keep, token={}",
                    maskedToken
            );
            return false;
        }

        MessagingErrorCode errorCode = exception.getMessagingErrorCode();
        return switch (errorCode) {
            case UNREGISTERED -> {
                log.warn(
                        "event=notification_token_send_failed, errorCode={}, action=deactivate, token={}",
                        errorCode,
                        maskedToken
                );
                yield true;
            }
            case INVALID_ARGUMENT -> {
                log.warn(
                        "event=notification_token_send_failed, errorCode={}, action=keep, "
                                + "reason=token_or_payload_invalid, token={}",
                        errorCode,
                        maskedToken
                );
                yield false;
            }
            case QUOTA_EXCEEDED, UNAVAILABLE -> {
                log.warn(
                        "event=notification_token_send_failed, errorCode={}, action=keep, "
                                + "retryRecommended=true, token={}",
                        errorCode,
                        maskedToken
                );
                yield false;
            }
            case INTERNAL -> {
                log.error(
                        "event=notification_token_send_failed, errorCode={}, action=keep, "
                                + "retryRecommended=true, token={}",
                        errorCode,
                        maskedToken
                );
                yield false;
            }
            case SENDER_ID_MISMATCH, THIRD_PARTY_AUTH_ERROR -> {
                log.error(
                        "event=notification_token_send_failed, errorCode={}, action=keep, "
                                + "reason=firebase_configuration, token={}",
                        errorCode,
                        maskedToken
                );
                yield false;
            }
        };
    }

    /**
     * 비활성화 요청 수와 실제 변경 행 수를 비교해 처리 결과를 기록합니다.
     *
     * <p>변경 행이 없으면 {@code not_found}, 일부만 변경되면 {@code partial},
     * 요청한 토큰이 모두 변경되면 {@code success}로 기록합니다.</p>
     *
     * @param requestedCount   비활성화를 요청한 고유 토큰 수
     * @param deactivatedCount 데이터베이스에서 실제 비활성화된 행 수
     */
    private void logDeactivationResult(int requestedCount, int deactivatedCount) {
        if (deactivatedCount == 0) {
            log.warn(
                    "event=notification_tokens_deactivated, result=not_found, "
                            + "requestedCount={}, deactivatedCount={}",
                    requestedCount,
                    deactivatedCount
            );
            return;
        }

        if (deactivatedCount < requestedCount) {
            log.warn(
                    "event=notification_tokens_deactivated, result=partial, "
                            + "requestedCount={}, deactivatedCount={}",
                    requestedCount,
                    deactivatedCount
            );
            return;
        }

        log.info(
                "event=notification_tokens_deactivated, result=success, "
                        + "requestedCount={}, deactivatedCount={}",
                requestedCount,
                deactivatedCount
        );
    }

    /**
     * 운영 로그에서 FCM 토큰 원문이 노출되지 않도록 마지막 여섯 글자만 남깁니다.
     *
     * @param fcmToken 마스킹할 FCM 토큰
     * @return 앞부분이 별표로 치환된 토큰 식별값
     */
    private String maskFcmToken(String fcmToken) {
        if (fcmToken == null || fcmToken.length() <= 6) {
            return "******";
        }
        return "******" + fcmToken.substring(fcmToken.length() - 6);
    }

}
