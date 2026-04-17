package org.zerock.puppyrun.discordNotice;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import static org.assertj.core.api.Assertions.assertThat;

public class DiscordNotificationTest {

    // 테스트 대상이 되는 Logger 객체를 가져옵니다.
    private final Logger testLogger = (Logger) LoggerFactory.getLogger(DiscordNotificationTest.class);

    // 로그 이벤트를 메모리에 저장할 가짜 Appender(ListAppender)를 선언합니다.
    private ListAppender<ILoggingEvent> listAppender;

    @BeforeEach
    void setUp() {
        // 각 테스트 실행 전에 ListAppender를 생성하고 시작합니다.
        listAppender = new ListAppender<>();
        listAppender.start();
        // 실제 로거에 가짜 Appender를 붙여 로그 이벤트를 가로챌 준비를 합니다.
        testLogger.addAppender(listAppender);
    }

    @AfterEach
    void tearDown() {
        // 테스트 종료 후에는 로거에서 Appender를 분리하여 다른 테스트에 영향을 주지 않도록 합니다.
        testLogger.detachAppender(listAppender);
    }

    @Test
    @DisplayName("ERROR 레벨 로그가 정상적으로 기록되는지 유닛 테스트")
    void discordAlarmUnitTest() {
        // given
        String errorMessage = "디스코드 알림 테스트 메시지입니다.";
        RuntimeException dummyException = new RuntimeException("테스트용 예외 발생!");
        String exceptionLogMessage = "예외 포함 알림 테스트";

        // when
        // 실제 로직처럼 log.error()를 호출합니다.
        testLogger.error(errorMessage);
        testLogger.error(exceptionLogMessage, dummyException);

        // then
        // ListAppender에 기록된 모든 로그 이벤트를 가져옵니다.
        List<ILoggingEvent> logsList = listAppender.list;

        // 1. 총 2개의 ERROR 로그가 기록되었는지 확인합니다.
        assertThat(logsList).hasSize(2);

        // 2. 첫 번째 로그의 레벨과 메시지를 검증합니다.
        ILoggingEvent firstLog = logsList.getFirst();
        assertThat(firstLog.getLevel().toString()).isEqualTo("ERROR");
        assertThat(firstLog.getFormattedMessage()).isEqualTo(errorMessage);
        assertThat(firstLog.getThrowableProxy()).isNull(); // 예외가 없어야 함

        // 3. 두 번째 로그에 예외 정보가 포함되었는지 검증합니다.
        ILoggingEvent secondLog = logsList.get(1);
        assertThat(secondLog.getLevel().toString()).isEqualTo("ERROR");
        assertThat(secondLog.getFormattedMessage()).isEqualTo(exceptionLogMessage);
        assertThat(secondLog.getThrowableProxy()).isNotNull(); // 예외가 있어야 함
        assertThat(secondLog.getThrowableProxy().getMessage()).isEqualTo(dummyException.getMessage());
    }
}
