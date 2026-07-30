package org.zerock.puppyrun.auth.oauth2.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.zerock.puppyrun.auth.oauth2.DTO.OAuth2Client;
import org.zerock.puppyrun.auth.oauth2.DTO.OAuth2UserProfile;
import org.zerock.puppyrun.auth.oauth2.controller.request.OAuth2SignInRequest;
import org.zerock.puppyrun.auth.oauth2.entity.SocialProvider;
import org.zerock.puppyrun.auth.oauth2.exception.OAuth2AuthenticationException;
import org.zerock.puppyrun.common.auth.jwt.JwtTokenProvider;
import org.zerock.puppyrun.member.entity.Member;
import org.zerock.puppyrun.member.exception.ExistingUserException;

@ExtendWith(MockitoExtension.class)
class SocialAuthServiceTest {

    @Mock
    private SocialClientService socialClientService;
    @Mock
    private SocialAccountService socialAccountService;
    @Mock
    private JwtTokenProvider jwtTokenProvider;

    private SocialAuthService socialAuthService;

    @BeforeEach
    void setUp() {
        socialAuthService = new SocialAuthService(
                socialClientService,
                socialAccountService,
                jwtTokenProvider
        );
    }

    @Test
    void 인가코드_검증부터_멤버_조회와_JWT_발급까지_순서대로_처리한다() {
        OAuth2SignInRequest request = request();
        OAuth2UserProfile profile = profile();
        Member member = member();
        when(socialClientService.authenticate(any(OAuth2Client.class))).thenReturn(profile);
        when(socialAccountService.findOrCreateMember(profile)).thenReturn(member);
        when(jwtTokenProvider.generateAccessToken(member.toDto())).thenReturn("access-token");
        when(jwtTokenProvider.generateRefreshToken(member.toDto())).thenReturn("refresh-token");

        var result = socialAuthService.signIn(request, SocialProvider.GOOGLE);

        ArgumentCaptor<OAuth2Client> clientCaptor = ArgumentCaptor.forClass(OAuth2Client.class);
        InOrder inOrder = inOrder(socialClientService, socialAccountService, jwtTokenProvider);
        inOrder.verify(socialClientService).authenticate(clientCaptor.capture());
        inOrder.verify(socialAccountService).findOrCreateMember(profile);
        inOrder.verify(jwtTokenProvider).generateAccessToken(member.toDto());
        inOrder.verify(jwtTokenProvider).generateRefreshToken(member.toDto());

        OAuth2Client client = clientCaptor.getValue();
        assertThat(client.provider()).isEqualTo(SocialProvider.GOOGLE);
        assertThat(client.authorizationCode()).isEqualTo(request.authorizationCode());
        assertThat(client.redirectUrl()).isEqualTo(request.redirectUrl());
        assertThat(result.accessToken()).isEqualTo("access-token");
        assertThat(result.refreshToken()).isEqualTo("refresh-token");
    }

    @Test
    void 소셜_제공자_인증에_실패하면_멤버를_조회하거나_JWT를_발급하지_않는다() {
        OAuth2AuthenticationException exception =
                new OAuth2AuthenticationException("인가 코드가 유효하지 않습니다.");
        when(socialClientService.authenticate(any(OAuth2Client.class))).thenThrow(exception);

        assertThatThrownBy(() -> socialAuthService.signIn(request(), SocialProvider.GOOGLE))
                .isSameAs(exception);
        verifyNoInteractions(socialAccountService, jwtTokenProvider);
    }

    @Test
    void 멤버_조회나_생성에_실패하면_JWT를_발급하지_않는다() {
        OAuth2UserProfile profile = profile();
        ExistingUserException exception = new ExistingUserException("계정 연결이 필요합니다.");
        when(socialClientService.authenticate(any(OAuth2Client.class))).thenReturn(profile);
        when(socialAccountService.findOrCreateMember(profile)).thenThrow(exception);

        assertThatThrownBy(() -> socialAuthService.signIn(request(), SocialProvider.GOOGLE))
                .isSameAs(exception);
        verifyNoInteractions(jwtTokenProvider);
    }

    private OAuth2SignInRequest request() {
        return new OAuth2SignInRequest(
                "authorization-code",
                "https://puppyrun.example.com/oauth2/callback"
        );
    }

    private OAuth2UserProfile profile() {
        return new OAuth2UserProfile(
                SocialProvider.GOOGLE,
                "google-user-id",
                "puppy@example.com",
                "puppy"
        );
    }

    private Member member() {
        return Member.builder()
                .email("puppy@example.com")
                .nickName("google_puppy")
                .password("encoded-password")
                .build();
    }
}
