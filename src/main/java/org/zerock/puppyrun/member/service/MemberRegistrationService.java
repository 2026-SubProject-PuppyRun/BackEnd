package org.zerock.puppyrun.member.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.zerock.puppyrun.member.DTO.MemberDTO;
import org.zerock.puppyrun.member.entity.Member;
import org.zerock.puppyrun.member.repository.MemberRepository;

/**
 * 일반 회원가입과 소셜 회원 생성에 공통으로 사용되는 회원 저장 서비스입니다.
 */
@Service
@RequiredArgsConstructor
public class MemberRegistrationService {
    private final MemberRepository memberRepository;

    /**
     * 일반 회원가입으로 회원을 저장합니다.
     *
     * @param email 회원 이메일
     * @param nickname 회원 닉네임
     * @param encodedPassword 암호화된 비밀번호
     * @return 저장된 회원 정보
     */
    @Transactional
    public MemberDTO registerLocalMember(String email, String nickname, String encodedPassword) {
        return saveMember(email, nickname, encodedPassword).toDto();
    }

    /**
     * 소셜 계정과 연결할 회원을 저장합니다.
     *
     * @param email 소셜 제공자가 전달한 이메일
     * @param nickname 생성할 회원 닉네임
     * @param encodedPassword 소셜 회원용 암호화 비밀번호
     * @return 소셜 계정 연결에 사용할 회원 엔티티
     */
    @Transactional
    public Member registerSocialMember(String email, String nickname, String encodedPassword) {
        return saveMember(email, nickname, encodedPassword);
    }

    private Member saveMember(String email, String nickname, String encodedPassword) {
        Member member = Member.builder()
                .email(email)
                .nickName(nickname)
                .password(encodedPassword)
                .build();
        return memberRepository.save(member);
    }
}
