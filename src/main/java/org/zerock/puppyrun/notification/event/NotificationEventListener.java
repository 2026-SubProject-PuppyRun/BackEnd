package org.zerock.puppyrun.notification.event;


import com.google.api.core.ApiFuture;
import com.google.api.core.ApiFutureCallback;
import com.google.api.core.ApiFutures;
import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.SendResponse;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.zerock.puppyrun.notification.service.DTO.PushTask;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationEventListener {
    // 구글이 허용하는 한 번의 최대 전송량
    private static final int MAX_FCM_BATCH_SIZE = 500;

    /**
     * FCM 메시지를 비동기적으로 일괄 발송합니다. 이 메서드는 호출 즉시 반환되며, 실제 전송은 백그라운드 스레드에서 처리됩니다.
     *
     * @param pushTasks 내용과 토큰이 각각 세팅된 Message 리스트
     */
    @Async("notificationTaskExecutor") // 이 메서드 자체를 별도의 스레드에서 실행하도록 지정
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
            });


        }
    }

    /**
     * FCM 토픽 방식을 사용하여 메시지를 비동기적으로 일괄 발송
     *
     * @param pushTask 발송할 메시지
     */
    @Async("notificationTaskExecutor")
    public void sendTopicMessage(PushTask pushTask) {
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
        });
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
                log.warn("발송 실패한 토큰 발견. 삭제 대상: {}, 원인: {}", failedToken,
                        responses.get(i).getException().getMessage());
                deadTokens.add(failedToken);

            }
        }

        if (!deadTokens.isEmpty()) {
            log.info("{}개의 만료된 토큰을 데이터베이스에서 비활성화 처리해야 합니다.", deadTokens.size());
            // TODO: deadTokens 리스트를 MemberRepository로 넘겨서 DB의 fcm_token을 NULL로 업데이트하는 로직을 구현 예정
        }
    }


}
