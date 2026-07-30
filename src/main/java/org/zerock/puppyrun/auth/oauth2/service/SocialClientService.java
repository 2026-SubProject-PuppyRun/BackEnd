package org.zerock.puppyrun.auth.oauth2.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.zerock.puppyrun.auth.oauth2.client.OAuth2ProviderStrategy;
import org.zerock.puppyrun.auth.oauth2.DTO.OAuth2Client;
import org.zerock.puppyrun.auth.oauth2.DTO.OAuth2UserProfile;
import org.zerock.puppyrun.auth.oauth2.factory.OAuth2ClientFactory;

@Service
@RequiredArgsConstructor
public class SocialClientService {
    private final OAuth2ClientFactory clientFactory;

    /**
     * 리다이렉트로 전달받은 인가 코드를 소셜 제공자에 검증하고 사용자 정보를 조회합니다.
     */
    public OAuth2UserProfile authenticate(OAuth2Client client) {

        OAuth2ProviderStrategy socialLogin = clientFactory.getStrategy(client.provider());

        String accessToken = socialLogin.exchangeAccessToken(client);

        return socialLogin.fetchUserProfile(accessToken);
    }
}
