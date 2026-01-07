package org.zerock.puppyrun.common.auth.security;

import java.util.UUID;
import org.zerock.puppyrun.member.entity.UserRole;

public record UserPrincipal(
        UUID id,
        String email,
        UserRole role
) {
}
