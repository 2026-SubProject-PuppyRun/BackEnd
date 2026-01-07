package org.zerock.puppyrun.member.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.zerock.puppyrun.common.auth.jwt.JwtTokenProvider;
import org.zerock.puppyrun.member.DTO.MemberDTO;
import org.zerock.puppyrun.member.DTO.TokenDTO;
import org.zerock.puppyrun.member.controller.request.SignUpRequest;
import org.zerock.puppyrun.member.entity.Member;
import org.zerock.puppyrun.member.exception.ExistingUserException;
import org.zerock.puppyrun.member.repository.MemberRepository;

@Service
@RequiredArgsConstructor
public class SignUpService {
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

    private Member saveMember(Member member) {
        return memberRepository.save(member);
    }

    /**
     * AccessToken 및 RefreshToken 생성
     *
     * @param memberDTO
     * @return TokenDTO
     */
    public TokenDTO crateToken(MemberDTO memberDTO) {
        return TokenDTO.builder()
                .accessToken(
                        jwtTokenProvider.generateAccessToken(memberDTO.id(), memberDTO.email(), memberDTO.userRole()))
                .refreshToken(
                        jwtTokenProvider.generateRefreshToken(memberDTO.id(), memberDTO.email(), memberDTO.userRole()))
                .build();
    }

    /**
     * 회원가입 처리 메서드
     *
     * @param request
     * @return MemberDTO
     */
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

        Member savedMember = saveMember(member);
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
}
