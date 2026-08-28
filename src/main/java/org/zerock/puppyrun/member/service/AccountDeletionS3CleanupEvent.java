package org.zerock.puppyrun.member.service;

import java.util.List;

/** 회원 탈퇴 DB 커밋 후 삭제할 S3 객체 목록입니다. */
public record AccountDeletionS3CleanupEvent(List<String> filePaths) {

    public AccountDeletionS3CleanupEvent {
        filePaths = List.copyOf(filePaths);
    }
}
