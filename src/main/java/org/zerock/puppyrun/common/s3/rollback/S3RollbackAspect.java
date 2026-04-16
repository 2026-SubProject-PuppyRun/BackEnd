package org.zerock.puppyrun.common.s3.rollback;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.zerock.puppyrun.common.s3.S3Service;
import org.zerock.puppyrun.common.s3.rollback.S3UploadRollback.S3RollbackEvent;

@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class S3RollbackAspect {

    private final ApplicationEventPublisher eventPublisher;
    private final S3Service s3Service;


    // @S3Rollback 어노테이션이 붙은 메서드의 실행 결과(URL)를 감시합니다.
    @AfterReturning(pointcut = "@annotation(org.zerock.puppyrun.common.s3.rollback.S3UploadRollback)", returning = "url")
    public void afterS3Upload(Object url) {
        if (url instanceof String s3Url) {
            // 메서드가 성공적으로 URL을 반환하면, 자동으로 이벤트를 발행합니다.
            eventPublisher.publishEvent(new S3RollbackEvent(s3Url));
        }
    }
}
