package org.zerock.puppyrun.member.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.zerock.puppyrun.common.s3.S3Service;

@Component
@RequiredArgsConstructor
@Slf4j
public class AccountDeletionS3CleanupHandler {
    private final S3Service s3Service;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(AccountDeletionS3CleanupEvent event) {
        log.info("회원 탈퇴 후 S3 이미지 삭제: {}건", event.filePaths().size());
        s3Service.deleteAll(event.filePaths());
    }
}
