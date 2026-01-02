package org.zerock.puppyrun.member.DTO;

import java.util.UUID;
import lombok.Builder;
import org.zerock.puppyrun.member.entity.Status;
import org.zerock.puppyrun.member.entity.UserRole;

@Builder
public record MemberDTO(
        UUID id,
        String nickName,
        String email,
        UserRole userRole,
        Status status
) {
}
