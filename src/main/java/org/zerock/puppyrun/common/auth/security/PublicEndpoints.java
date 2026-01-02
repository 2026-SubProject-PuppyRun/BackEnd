package org.zerock.puppyrun.common.auth.security;

import java.util.List;

public final class PublicEndpoints {
    public static final List<String> ALLOWED = List.of(
            "/api/auth/check-email",       // 이메일 중복 체크
            "/api/auth/check-nickname",    // 닉네임 중복 체크
            "/api/auth/sign-up",           // 회원가입
            "/api/auth/sign-in",           // 로그인
            "/api/auth/find-id",           // 아이디 찾기
            "/api/auth/reset-password",    // 비밀번호 재설정
            "/api/verification/confirm",    // 본인 확인
            "/api/auth/refresh"             // 토큰 갱신
    );
}
