package org.zerock.puppyrun.member.service;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.zerock.puppyrun.auth.service.AuthService;
import org.zerock.puppyrun.common.exception.InvalidValueException;
import org.zerock.puppyrun.member.DTO.MemberDTO;
import org.zerock.puppyrun.member.entity.Member;
import org.zerock.puppyrun.member.exception.UserNotFoundException;
import org.zerock.puppyrun.member.exception.UserUnauthorizedException;
import org.zerock.puppyrun.member.repository.MemberRepository;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class MemberService {
    private final AuthService authService;
    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * 비밀번호 일치 여부 확인
     *
     * @param encodedPassword 기존 암호화 된 비밀번호
     * @param rawPassword     새로운 비밀번호
     */
    private void matchPassword(String rawPassword, String encodedPassword) {
        if (!passwordEncoder.matches(rawPassword, encodedPassword)) {
            log.warn("비밀번호가 일치하지 않습니다.");
            throw new UserUnauthorizedException("비밀번호가 틀립니다.");
        }
    }

    /**
     * 이메일 마스킹 처리
     */
    public String maskEmail(String email) {
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
     * 회원 정보 조회
     *
     * @param id
     * @return MemberDTO
     */
    public MemberDTO memberInformationView(UUID id) {
        return memberRepository.findByIdOrThrow(id).toDto();
    }

    /**
     * @param id          유저 고유 아이디
     * @param oldPassword 기존 비밀번호
     * @param newPassword 새로운 비밀번호
     * @return MemberDTO
     */
    @Transactional
    public MemberDTO passwordChange(UUID id, String oldPassword, String newPassword) {

        if (oldPassword.equals(newPassword)) {
            throw new InvalidValueException("기존 비밀번호와 동일합니다.");
        }

        Member member = memberRepository.findByIdOrThrow(id);
        String encodedPassword = member.getPassword();
        matchPassword(oldPassword, encodedPassword); // 현재 비밀번호 검증
        String encryptedPassword = passwordEncoder.encode(newPassword);
        member.updatePassword(encryptedPassword);

        return member.toDto();
    }

    /**
     * 닉네임 변경
     *
     * @param id          유저 고유 아이디
     * @param newNickName 새로운 닉네임
     * @return MemberDTO
     */
    @Transactional
    public MemberDTO nickNameChange(UUID id, String newNickName) {
        Member member = memberRepository.findByIdOrThrow(id);
        if (memberRepository.existsByNickName(newNickName)) {
            throw new InvalidValueException("이미 존재하는 닉네임입니다.");
        }

        member.updateNickName(newNickName);
        return member.toDto();
    }

    /**
     * 계정 삭제
     *
     * @param id       유저 고유 아이디
     * @param password 비밀번호
     */
    @Transactional
    public void accountDelete(UUID id, String password) {
        // TODO 나중에 기능 개발 할 예정
        Member member = memberRepository.findByIdOrThrow(id);
        String encodedPassword = member.getPassword();
        matchPassword(password, encodedPassword);
        member.setDeactivate();
    }

}
