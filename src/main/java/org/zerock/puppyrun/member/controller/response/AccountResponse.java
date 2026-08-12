package org.zerock.puppyrun.member.controller.response;

import lombok.Builder;
import org.zerock.puppyrun.common.s3.support.S3Url;

@Builder
public record AccountResponse(
        String nickName,
        String email,
        @S3Url
        String profileImage,
        String UserRole
) {
}
