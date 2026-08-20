package org.zerock.puppyrun.auth.oauth2.service;

import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.zerock.puppyrun.auth.oauth2.entity.SocialAccount;
import org.zerock.puppyrun.auth.oauth2.DTO.OAuth2UserProfile;
import org.zerock.puppyrun.auth.oauth2.exception.OAuth2AuthenticationException;
import org.zerock.puppyrun.auth.oauth2.repository.SocialAccountRepository;
import org.zerock.puppyrun.member.DTO.MemberDTO;
import org.zerock.puppyrun.member.entity.Member;
import org.zerock.puppyrun.member.exception.ExistingUserException;
import org.zerock.puppyrun.member.service.MemberQueryService;
import org.zerock.puppyrun.member.service.MemberRegistrationService;

/**
 * 소셜 계정과 회원의 조회 및 저장을 담당하는 서비스입니다.
 *
 * <p>소셜 제공자의 사용자 식별자로 기존 계정을 찾고, 계정이 없으면 회원과
 * 소셜 계정을 하나의 트랜잭션에서 생성합니다.</p>
 */
@Component
@RequiredArgsConstructor
public class SocialAccountService {
    private static final int MAX_NICKNAME_LENGTH = 20;

    private final SocialAccountRepository socialAccountRepository;
    private final MemberQueryService memberQueryService;
    private final MemberRegistrationService memberRegistrationService;
    private final PasswordEncoder passwordEncoder;

    /**
     * 소셜 프로필에 연결된 회원을 조회하거나 신규 회원을 생성합니다.
     *
     * @param profile 소셜 제공자에서 조회한 공통 사용자 프로필
     * @return 기존 또는 신규 회원 정보
     * @throws OAuth2AuthenticationException 프로필의 제공자, 사용자 식별자 또는 이메일이 없는 경우
     * @throws ExistingUserException         동일한 이메일을 사용하는 기존 회원이 있어 자동 연결할 수 없는 경우
     */
    @Transactional
    public Member findOrCreateMember(OAuth2UserProfile profile) {
        return socialAccountRepository
                .findByProviderAndProviderUserId(
                        profile.provider(),
                        profile.providerUserId()
                )
                .map(SocialAccount::getMember)
                .orElseGet(() -> createSocialMember(profile));
    }

    private Member createSocialMember(OAuth2UserProfile profile) {
        if (memberQueryService.existsByEmail(profile.email())) {
            throw new ExistingUserException("동일한 이메일의 기존 계정이 존재합니다. 계정 연결이 필요합니다.");
        }

        Member member = memberRegistrationService.registerSocialMember(
                profile.email(),
                resolveNickName(profile),
                passwordEncoder.encode(UUID.randomUUID().toString())
        );

        SocialAccount socialAccount = SocialAccount.builder()
                .member(member)
                .provider(profile.provider())
                .providerUserId(profile.providerUserId())
                .build();
        socialAccountRepository.save(socialAccount);

        return member;
    }

    private String resolveNickName(OAuth2UserProfile profile) {
        String provider = profile.provider()
                .name()
                .toLowerCase(Locale.ROOT);
        String nickname;

        do {
            String randomValue = UUID.randomUUID()
                    .toString()
                    .replace("-", "")
                    .substring(0, 6);
            nickname = provider + "_" + randomValue;
        } while (memberQueryService.existsByNickname(nickname));
        return nickname;
    }

    private void requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new OAuth2AuthenticationException(message);
        }
    }


}
