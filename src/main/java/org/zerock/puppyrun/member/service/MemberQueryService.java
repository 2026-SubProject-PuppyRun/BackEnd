package org.zerock.puppyrun.member.service;

import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.zerock.puppyrun.member.DTO.MemberCredentials;
import org.zerock.puppyrun.member.DTO.MemberDTO;
import org.zerock.puppyrun.member.repository.MemberRepository;

/**
 * 다른 기능에서 필요한 회원 조회 기능을 제공하는 서비스입니다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberQueryService {
    private final MemberRepository memberRepository;

    /**
     * 닉네임 사용 여부를 확인합니다.
     *
     * @param nickname 확인할 닉네임
     * @return 이미 사용 중이면 {@code true}
     */
    public boolean existsByNickname(String nickname) {
        return memberRepository.existsByNickName(nickname);
    }

    /**
     * 이메일 사용 여부를 확인합니다.
     *
     * @param email 확인할 이메일
     * @return 이미 사용 중이면 {@code true}
     */
    public boolean existsByEmail(String email) {
        return memberRepository.existsByEmail(email);
    }

    /**
     * 이메일에 해당하는 인증용 회원 정보를 조회합니다.
     *
     * @param email 조회할 회원 이메일
     * @return 회원 정보와 암호화 비밀번호, 존재하지 않으면 빈 값
     */
    public Optional<MemberCredentials> findCredentialsByEmail(String email) {
        return memberRepository.findByEmail(email)
                .map(member -> new MemberCredentials(member.toDto(), member.getPassword()));
    }

    /**
     * 식별자에 해당하는 회원을 조회합니다.
     *
     * @param id 회원 식별자
     * @return 회원 정보
     */
    public MemberDTO findByIdOrThrow(UUID id) {
        return memberRepository.findByIdOrThrow(id).toDto();
    }
}
