package org.zerock.puppyrun.auth.local.service;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.zerock.puppyrun.common.auth.jwt.JwtTokenProvider;
import org.zerock.puppyrun.member.DTO.MemberDTO;
import org.zerock.puppyrun.auth.DTO.TokenDTO;
import org.zerock.puppyrun.auth.local.controller.request.SignInRequest;
import org.zerock.puppyrun.auth.local.controller.request.SignUpRequest;
import org.zerock.puppyrun.member.exception.ExistingUserException;
import org.zerock.puppyrun.member.exception.UserNotFoundException;
import org.zerock.puppyrun.member.exception.UserUnauthorizedException;
import org.zerock.puppyrun.member.service.MemberQueryService;
import org.zerock.puppyrun.member.service.MemberRegistrationService;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {
    private final MemberQueryService memberQueryService;
    private final MemberRegistrationService memberRegistrationService;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;


    // 닉네임 중복 검증
    public boolean isExistsByNickname(String nickName) {
        return memberQueryService.existsByNickname(nickName);
    }

    // 이메일 중복 검증
    public boolean isExistsByEmail(String email) {
        return memberQueryService.existsByEmail(email);
    }

    protected MemberDTO findMemberById(UUID id) {
        return memberQueryService.findByIdOrThrow(id);
    }

    /**
     * AccessToken 및 RefreshToken 생성
     *
     * @param memberDTO 토큰에 포함할 회원 정보
     * @return 액세스 토큰과 리프레시 토큰
     */
    public TokenDTO crateToken(MemberDTO memberDTO) {
        return TokenDTO.builder()
                .accessToken(jwtTokenProvider.generateAccessToken(memberDTO))
                .refreshToken(jwtTokenProvider.generateRefreshToken(memberDTO))
                .build();
    }

    /**
     * 회원가입 처리 메서드
     *
     * @param request 회원가입 요청
     * @return 생성된 회원 정보
     */
    @Transactional
    public MemberDTO registrarMember(SignUpRequest request) {
        if (isExistsByNickname(request.nickName())) {
            throw new ExistingUserException("이미 존재하는 닉네임입니다. : " + request.nickName());
        }
        if (isExistsByEmail(request.email())) {
            throw new ExistingUserException("이미 존재하는 이메일입니다. : " + request.email());
        }

        // 비밀번호 암호화
        String encryptedPassword = passwordEncoder.encode(request.password());

        return memberRegistrationService.registerLocalMember(
                request.email(),
                request.nickName(),
                encryptedPassword
        );
    }

    /**
     * 로그인 처리 (AccessToken,RefreshToken 포함)
     *
     * @param request 이메일과 비밀번호를 포함한 로그인 요청
     * @return 액세스 토큰과 리프레시 토큰
     */
    public TokenDTO signIn(SignInRequest request) {
        return memberQueryService.findCredentialsByEmail(request.email())
                .map(credentials -> {
                    // 비밀번호 검증
                    if (!passwordEncoder.matches(request.password(), credentials.encodedPassword())) {
                        throw new UserUnauthorizedException("비밀번호가 틀립니다.");
                    }
                    // 토큰 쌍 생성 (Access + Refresh)
                    return crateToken(credentials.member());
                })
                .orElseThrow(() -> new UserNotFoundException("존재하지 않는 회원입니다."));
    }

    /**
     * AccessToken 재발급
     *
     * @param refreshToken 사용자 식별에 사용할 리프레시 토큰
     * @return 새 액세스 토큰과 리프레시 토큰
     */
    public TokenDTO AccessTokenReissuance(String refreshToken) {
        UUID userId = jwtTokenProvider.getUserIdFromRefreshToken(refreshToken);
        MemberDTO member = findMemberById(userId);

        return TokenDTO.builder()
                .accessToken(jwtTokenProvider.generateAccessToken(member))
                .refreshToken(jwtTokenProvider.generateRefreshToken(member))
                .build();
    }

}
