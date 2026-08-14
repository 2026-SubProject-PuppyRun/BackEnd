package org.zerock.puppyrun.notification.client;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.SendResponse;
import java.util.List;
import java.util.concurrent.Executor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.zerock.puppyrun.notification.entity.NotificationType;
import org.zerock.puppyrun.notification.repository.NotificationRepository;
import org.zerock.puppyrun.notification.client.DTO.PushTask;
import org.zerock.puppyrun.notification.client.DTO.TokenPushTask;

@ExtendWith(MockitoExtension.class)
class NotificationEventClientTest {

    private static final String FCM_TOKEN = "test-fcm-token";

    @Mock
    private NotificationRepository notificationRepository;


    @Test
    @DisplayName("FCM에서 해제된 토큰은 데이터베이스에서 비활성화한다")
    void deactivateUnregisteredToken() {
        // given
        NotificationEventClient client = createClient();
        BatchResponse batchResponse = failedBatchResponse(MessagingErrorCode.UNREGISTERED);
        when(notificationRepository.deactivateActiveTokensByFcmToken(List.of(FCM_TOKEN)))
                .thenReturn(1);

        // when
        client.handleFailedTokens(List.of(tokenPushTask()), batchResponse);

        // then
        verify(notificationRepository).deactivateActiveTokensByFcmToken(List.of(FCM_TOKEN));
    }

    @ParameterizedTest(name = "{0} 오류는 토큰을 비활성화하지 않는다")
    @EnumSource(
            value = MessagingErrorCode.class,
            names = "UNREGISTERED",
            mode = EnumSource.Mode.EXCLUDE
    )
    void keepTokenWhenFailureIsNotUnregistered(MessagingErrorCode errorCode) {
        // given
        NotificationEventClient client = createClient();
        BatchResponse batchResponse = failedBatchResponse(errorCode);

        // when
        client.handleFailedTokens(List.of(tokenPushTask()), batchResponse);

        // then
        verify(notificationRepository, never()).deactivateActiveTokensByFcmToken(List.of(FCM_TOKEN));
    }

    @Test
    @DisplayName("FCM 오류 코드를 확인할 수 없으면 토큰을 유지한다")
    void keepTokenWhenErrorCodeIsUnknown() {
        // given
        NotificationEventClient client = createClient();
        BatchResponse batchResponse = failedBatchResponse(null);

        // when
        client.handleFailedTokens(List.of(tokenPushTask()), batchResponse);

        // then
        verify(notificationRepository, never()).deactivateActiveTokensByFcmToken(List.of(FCM_TOKEN));
    }

    private NotificationEventClient createClient() {
        Executor directExecutor = Runnable::run;
        return new NotificationEventClient(
                directExecutor,
                notificationRepository
        );
    }

    private BatchResponse failedBatchResponse(MessagingErrorCode errorCode) {
        FirebaseMessagingException exception = mock(FirebaseMessagingException.class);
        when(exception.getMessagingErrorCode()).thenReturn(errorCode);

        SendResponse sendResponse = mock(SendResponse.class);
        when(sendResponse.isSuccessful()).thenReturn(false);
        when(sendResponse.getException()).thenReturn(exception);

        BatchResponse batchResponse = mock(BatchResponse.class);
        when(batchResponse.getResponses()).thenReturn(List.of(sendResponse));
        return batchResponse;
    }

    private PushTask tokenPushTask() {
        return new TokenPushTask(
                FCM_TOKEN,
                NotificationType.DAILY_WALKING_REMINDER,
                "테스트 알림",
                "테스트 본문"
        );
    }
}
