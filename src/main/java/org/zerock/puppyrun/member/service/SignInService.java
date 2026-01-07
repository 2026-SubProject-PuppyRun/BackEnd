package org.zerock.puppyrun.member.service;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.zerock.puppyrun.common.auth.jwt.JwtTokenProvider;
import org.zerock.puppyrun.member.DTO.TokenDTO;
import org.zerock.puppyrun.member.controller.request.SignInRequest;
import org.zerock.puppyrun.member.entity.UserRole;
import org.zerock.puppyrun.member.exception.UserNotFoundException;
import org.zerock.puppyrun.member.exception.UserUnauthorizedException;
import org.zerock.puppyrun.member.repository.MemberRepository;

@Service
@RequiredArgsConstructor
public class SignInService {
    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    /*
     * 로그인 처리 (AccessToken,RefreshToken 포함)
     */
    @Transactional(readOnly = true)
    public TokenDTO signIn(SignInRequest request) {
        return memberRepository.findByEmail(request.email())
                .map(member -> {
                    // 비밀번호 검증
                    if (!passwordEncoder.matches(request.password(), member.getPassword())) {
                        throw new UserUnauthorizedException("비밀번호가 틀립니다.");
                    }

                    UUID id = member.toDto().id();
                    UserRole userRole = member.toDto().userRole();

                    // 토큰 쌍 생성 (Access + Refresh)
                    return TokenDTO.builder()
                            .accessToken(jwtTokenProvider.generateAccessToken(id, userRole))
                            .refreshToken(jwtTokenProvider.generateRefreshToken(id, userRole))
                            .build();
                })
                .orElseThrow(() -> new UserNotFoundException("존재하지 않는 회원입니다."));
    }

}
