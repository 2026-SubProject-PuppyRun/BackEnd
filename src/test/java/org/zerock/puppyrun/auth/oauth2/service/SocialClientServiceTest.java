package org.zerock.puppyrun.auth.oauth2.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.zerock.puppyrun.auth.oauth2.DTO.OAuth2Client;
import org.zerock.puppyrun.auth.oauth2.DTO.OAuth2UserProfile;
import org.zerock.puppyrun.auth.oauth2.client.OAuth2ProviderStrategy;
import org.zerock.puppyrun.auth.oauth2.entity.SocialProvider;
import org.zerock.puppyrun.auth.oauth2.factory.OAuth2ClientFactory;

@ExtendWith(MockitoExtension.class)
class SocialClientServiceTest {

    @Mock
    private OAuth2ClientFactory clientFactory;
    @Mock
    private OAuth2ProviderStrategy providerStrategy;

    private SocialClientService socialClientService;

    @BeforeEach
    void setUp() {
        socialClientService = new SocialClientService(clientFactory);
    }

    @Test
    void 제공자_전략을_찾아_토큰을_교환하고_사용자_프로필을_조회한다() {
        OAuth2Client client = new OAuth2Client(
                SocialProvider.KAKAO,
                "authorization-code",
                "https://puppyrun.example.com/oauth2/callback"
        );
        OAuth2UserProfile profile = new OAuth2UserProfile(
                SocialProvider.KAKAO,
                "kakao-user-id",
                "puppy@example.com",
                "puppy"
        );
        when(clientFactory.getStrategy(SocialProvider.KAKAO)).thenReturn(providerStrategy);
        when(providerStrategy.exchangeAccessToken(client)).thenReturn("provider-access-token");
        when(providerStrategy.fetchUserProfile("provider-access-token")).thenReturn(profile);

        OAuth2UserProfile result = socialClientService.authenticate(client);

        InOrder inOrder = inOrder(clientFactory, providerStrategy);
        inOrder.verify(clientFactory).getStrategy(SocialProvider.KAKAO);
        inOrder.verify(providerStrategy).exchangeAccessToken(client);
        inOrder.verify(providerStrategy).fetchUserProfile("provider-access-token");
        assertThat(result).isSameAs(profile);
    }
}
