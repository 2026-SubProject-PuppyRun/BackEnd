package org.zerock.puppyrun.common.s3.rollback;

import java.util.List;

/**
 * S3 업로드 작업에 대한 롤백 정보를 담는 이벤트 클래스입니다.
 * DB 트랜잭션 실패 시 해당 경로의 파일을 S3에서 삭제하기 위해 사용됩니다.
 */
public record S3RollbackEvent(List<String> filePaths) {

    /**
     * 단일 파일 경로로 이벤트를 생성합니다.
     */
    public static S3RollbackEvent of(String filePath) {
        return new S3RollbackEvent(List.of(filePath));
    }

    /**
     * 여러 파일 경로 리스트로 이벤트를 생성합니다.
     */
    public static S3RollbackEvent listOf(List<String> filePaths) {
        return new S3RollbackEvent(filePaths);
    }
}
