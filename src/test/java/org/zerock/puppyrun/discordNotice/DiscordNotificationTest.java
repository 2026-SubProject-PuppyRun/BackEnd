package org.zerock.puppyrun.discordNotice;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class DiscordNotificationTest {

    // 로그백 로거 설정
    private static final Logger log = LoggerFactory.getLogger(DiscordNotificationTest.class);

    @Test
    @DisplayName("디스코드 에러 알림 전송 테스트")
    void discordAlarmTest() throws InterruptedException {
        // ERROR 레벨 로그 기록
        log.error("🚨 [Test] 디스코드 알림 테스트 메시지입니다.");
        log.error("사유: 시스템 테스트 중 강제 에러 발생");

        // 예외 객체를 포함한 로그 테스트 (스택 트레이스 확인)
        RuntimeException dummyException = new RuntimeException("테스트용 예외 발생!");
        log.error("🚨 [Test] 예외 포함 알림 테스트", dummyException);

        // 전송되기 전에 테스트가 종료될 수 있으므로 잠시 대기
        System.out.println("디스코드로 로그를 전송 중입니다... 채널을 확인하세요.");
        Thread.sleep(5000);
    }
}
