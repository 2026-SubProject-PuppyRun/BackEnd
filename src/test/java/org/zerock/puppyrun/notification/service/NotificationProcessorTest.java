package org.zerock.puppyrun.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.zerock.puppyrun.common.pagination.SliceResult;
import org.zerock.puppyrun.notification.client.NotificationEventClient;
import org.zerock.puppyrun.notification.client.DTO.PushTask;
import org.zerock.puppyrun.notification.entity.NotificationType;
import org.zerock.puppyrun.notification.repository.DTO.EnabledNotifications;
import org.zerock.puppyrun.notification.repository.NotificationRepository;
import org.zerock.puppyrun.notification.service.sender.Sender;

@ExtendWith(MockitoExtension.class)
class NotificationProcessorTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private NotificationEventClient notificationEventClient;

    @Mock
    private Sender sender;

    @InjectMocks
    private NotificationProcessor notificationProcessor;

    @Test
    @SuppressWarnings("unchecked")
    @DisplayName("다음 페이지 확인용 추가 한 명은 다음 조회로 넘기고 정확히 페이지 크기만 발송한다")
    void broadcastSendsOnlyPageSizeWhenMoreRecipientsExist() {
        // given
        List<EnabledNotifications> firstPage = IntStream.range(0, 1_000)
                .mapToObj(this::recipient)
                .toList();
        EnabledNotifications remainingRecipient = recipient(1_001);
        PushTask pushTask = org.mockito.Mockito.mock(PushTask.class);
        given(notificationRepository.findNextMembers(any(), any(), any(Pageable.class), any()))
                .willReturn(
                        new SliceResult<>(firstPage, true),
                        new SliceResult<>(List.of(remainingRecipient), false)
                );
        given(sender.createPushTasks(any()))
                .willAnswer(invocation -> {
                    List<EnabledNotifications> recipients = invocation.getArgument(0);
                    return java.util.Collections.nCopies(recipients.size(), pushTask);
                });

        // when
        notificationProcessor.broadcast(
                NotificationType.DAILY_WALKING_REMINDER,
                sender
        );

        // then
        ArgumentCaptor<List<PushTask>> pushTasksCaptor = pushTaskListCaptor();
        verify(notificationEventClient, times(2)).sendMessagesInBulk(pushTasksCaptor.capture());
        assertThat(pushTasksCaptor.getAllValues())
                .extracting(List::size)
                .containsExactly(1_000, 1);
        verify(notificationRepository).findNextMembers(
                eq(firstPage.get(999).createdAt()),
                eq(firstPage.get(999).memberId()),
                any(Pageable.class),
                eq(NotificationType.DAILY_WALKING_REMINDER)
        );
    }

    @Test
    @DisplayName("대상자 조회 예외는 호출자에게 전파한다")
    void broadcastPropagatesRecipientQueryFailure() {
        // given
        given(notificationRepository.findNextMembers(any(), any(), any(Pageable.class), any()))
                .willThrow(new IllegalStateException("database unavailable"));

        // when & then
        assertThatThrownBy(() -> notificationProcessor.broadcast(
                NotificationType.DAILY_WALKING_REMINDER,
                sender
        )).isInstanceOf(IllegalStateException.class)
                .hasMessage("database unavailable");
    }

    private EnabledNotifications recipient(int index) {
        return EnabledNotifications.builder()
                .memberId(UUID.randomUUID())
                .type(NotificationType.DAILY_WALKING_REMINDER)
                .fcmToken("fcm-token-" + index)
                .createdAt(LocalDateTime.of(2026, 8, 18, 20, 0).plusNanos(index))
                .build();
    }

    @SuppressWarnings("unchecked")
    private ArgumentCaptor<List<PushTask>> pushTaskListCaptor() {
        return ArgumentCaptor.forClass(List.class);
    }
}
