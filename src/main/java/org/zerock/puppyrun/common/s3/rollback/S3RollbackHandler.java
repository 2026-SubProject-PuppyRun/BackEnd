package org.zerock.puppyrun.common.s3.rollback;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.zerock.puppyrun.common.s3.S3Service;

@Component
@RequiredArgsConstructor
@Slf4j
public class S3RollbackHandler {
    private final S3Service s3Service;

    // phase = AFTER_ROLLBACK: DB 트랜잭션이 최종적으로 실패했을 때만 실행
    @TransactionalEventListener(phase = TransactionPhase.AFTER_ROLLBACK)
    public void handleRollback(S3RollbackEvent event) {
        log.warn("DB 저장 트랜잭션 실패로 인해 S3 파일을 롤백합니다: {} 건", event.filePaths().size());
        s3Service.deleteAll(event.filePaths());
    }
}
