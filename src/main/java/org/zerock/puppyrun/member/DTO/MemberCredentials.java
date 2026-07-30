package org.zerock.puppyrun.member.DTO;

/**
 * 인증에 필요한 회원 정보와 암호화된 비밀번호입니다.
 *
 * @param member 토큰 발급에 사용할 회원 정보
 * @param encodedPassword 저장된 암호화 비밀번호
 */
public record MemberCredentials(
        MemberDTO member,
        String encodedPassword
) {
}
