package org.zerock.puppyrun.auth.oauth2.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.zerock.puppyrun.auth.DTO.TokenDTO;
import org.zerock.puppyrun.auth.oauth2.client.OAuth2ProviderStrategy;
import org.zerock.puppyrun.auth.oauth2.controller.request.OAuth2SignInRequest;
import org.zerock.puppyrun.auth.oauth2.entity.SocialProvider;
import org.zerock.puppyrun.auth.oauth2.DTO.OAuth2UserProfile;
import org.zerock.puppyrun.auth.oauth2.factory.OAuth2ClientFactory;
import org.zerock.puppyrun.common.auth.jwt.JwtTokenProvider;
import org.zerock.puppyrun.member.DTO.MemberDTO;

/**
 * 소셜 인증부터 회원 조회·가입 및 PuppyRun 토큰 발급까지 조율합니다.
 */
@Service
@RequiredArgsConstructor
public class SocialAuthService {
    private final SocialAccountService socialAccountService;
    private final JwtTokenProvider jwtTokenProvider;
    private final OAuth2ClientFactory clientFactory;


    /**
     * 제공자 액세스 토큰 검증, 회원 조회·가입, PuppyRun 토큰 발급 순서로 소셜 로그인을 처리합니다.
     *
     * @param request  클라이언트가 전달한 제공자 액세스 토큰
     * @param provider URL 경로에서 결정된 소셜 로그인 제공자
     * @return PuppyRun 액세스 토큰과 리프레시 토큰
     */
    public TokenDTO signIn(OAuth2SignInRequest request, SocialProvider provider) {
        OAuth2ProviderStrategy socialLogin = clientFactory.getStrategy(provider);
        OAuth2UserProfile profile = socialLogin.fetchUserProfile(request.providerAccessToken());
        MemberDTO member = socialAccountService.findOrCreateMember(profile).toDto();

        return TokenDTO.builder()
                .accessToken(jwtTokenProvider.generateAccessToken(member))
                .refreshToken(jwtTokenProvider.generateRefreshToken(member))
                .build();
    }


}
