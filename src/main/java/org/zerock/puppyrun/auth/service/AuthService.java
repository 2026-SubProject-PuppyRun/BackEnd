package org.zerock.puppyrun.auth.service;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.zerock.puppyrun.common.auth.jwt.JwtTokenProvider;
import org.zerock.puppyrun.member.DTO.MemberDTO;
import org.zerock.puppyrun.auth.DTO.TokenDTO;
import org.zerock.puppyrun.auth.controller.request.SignInRequest;
import org.zerock.puppyrun.auth.controller.request.SignUpRequest;
import org.zerock.puppyrun.member.entity.Member;
import org.zerock.puppyrun.member.exception.ExistingUserException;
import org.zerock.puppyrun.member.exception.UserNotFoundException;
import org.zerock.puppyrun.member.exception.UserUnauthorizedException;
import org.zerock.puppyrun.member.repository.MemberRepository;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {
    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;


    // 닉네임 중복 검증
    public boolean isExistsByNickname(String nickName) {
        return memberRepository.existsByNickName(nickName);
    }

    // 이메일 중복 검증
    public boolean isExistsByEmail(String email) {
        return memberRepository.existsByEmail(email);
    }

    protected Member findMemberById(UUID id) {
        return memberRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("존재하지 않는 회원입니다."));
    }

    /**
     * AccessToken 및 RefreshToken 생성
     *
     * @param MemberDTO
     * @return TokenDTO
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
     * @param SignUpRequest
     * @return MemberDTO
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

        Member member = Member.builder()
                .email(request.email())
                .nickName(request.nickName())
                .password(encryptedPassword)
                .build();

        Member savedMember = memberRepository.save(member);
        return savedMember.toDto();
    }

    /**
     * 이메일 마스킹 처리
     */
    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return null;
        }

        String[] parts = email.split("@");
        String localPart = parts[0];
        String domain = parts[1];

        // 앞부분의 처음 1-2자만 보이게 하고 나머지는 ****로 마스킹
        String maskedLocal;
        if (localPart.length() <= 2) {
            maskedLocal = localPart.charAt(0) + "****";
        } else {
            maskedLocal = localPart.substring(0, 2) + "****";
        }

        return maskedLocal + "@" + domain;
    }

    /**
     * 로그인 처리 (AccessToken,RefreshToken 포함)
     *
     * @param SignInRequest
     * @return TokenDTO
     */
    public TokenDTO signIn(SignInRequest request) {
        return memberRepository.findByEmail(request.email())
                .map(member -> {
                    // 비밀번호 검증
                    if (!passwordEncoder.matches(request.password(), member.getPassword())) {
                        throw new UserUnauthorizedException("비밀번호가 틀립니다.");
                    }

                    // 토큰 쌍 생성 (Access + Refresh)
                    return TokenDTO.builder()
                            .accessToken(jwtTokenProvider.generateAccessToken(member.toDto()))
                            .refreshToken(jwtTokenProvider.generateRefreshToken(member.toDto()))
                            .build();
                })
                .orElseThrow(() -> new UserNotFoundException("존재하지 않는 회원입니다."));
    }

    /**
     * AccessToken 재발급
     *
     * @return TokenDTO
     */
    public TokenDTO AccessTokenReissuance(String refreshToken) {
        UUID userId = jwtTokenProvider.getUserIdFromRefreshToken(refreshToken);

        Member member = findMemberById(userId);

        String newAccessToken = jwtTokenProvider.generateAccessToken(member.toDto());
        String newRefreshToken = jwtTokenProvider.generateRefreshToken(member.toDto());

        return TokenDTO.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken) // 새 리프레시 토큰 반환
                .build();
    }


}
