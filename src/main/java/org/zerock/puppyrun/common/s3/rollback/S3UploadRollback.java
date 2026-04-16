package org.zerock.puppyrun.common.s3.rollback;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface S3UploadRollback {
    public record S3RollbackEvent(String filePath) {
    }
}
