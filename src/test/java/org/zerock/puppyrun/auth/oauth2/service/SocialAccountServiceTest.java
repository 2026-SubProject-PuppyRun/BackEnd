package org.zerock.puppyrun.auth.oauth2.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.zerock.puppyrun.auth.oauth2.DTO.OAuth2UserProfile;
import org.zerock.puppyrun.auth.oauth2.entity.SocialAccount;
import org.zerock.puppyrun.auth.oauth2.entity.SocialProvider;
import org.zerock.puppyrun.auth.oauth2.exception.OAuth2AuthenticationException;
import org.zerock.puppyrun.auth.oauth2.repository.SocialAccountRepository;
import org.zerock.puppyrun.member.entity.Member;
import org.zerock.puppyrun.member.exception.ExistingUserException;
import org.zerock.puppyrun.member.service.MemberQueryService;
import org.zerock.puppyrun.member.service.MemberRegistrationService;

@ExtendWith(MockitoExtension.class)
class SocialAccountServiceTest {

    @Mock
    private SocialAccountRepository socialAccountRepository;
    @Mock
    private MemberQueryService memberQueryService;
    @Mock
    private MemberRegistrationService memberRegistrationService;
    @Mock
    private PasswordEncoder passwordEncoder;

    private SocialAccountService socialAccountService;

    @BeforeEach
    void setUp() {
        socialAccountService = new SocialAccountService(
                socialAccountRepository,
                memberQueryService,
                memberRegistrationService,
                passwordEncoder
        );
    }

    @Test
    void 소셜_계정이_존재하면_연결된_멤버를_그대로_반환한다() {
        OAuth2UserProfile profile = profile();
        Member member = member(profile.email(), "google_existing");
        SocialAccount socialAccount = socialAccount(profile, member);
        when(socialAccountRepository.findByProviderAndProviderUserId(
                profile.provider(),
                profile.providerUserId()
        )).thenReturn(Optional.of(socialAccount));

        Member result = socialAccountService.findOrCreateMember(profile);

        assertThat(result).isSameAs(member);
        verify(socialAccountRepository).findByProviderAndProviderUserId(
                SocialProvider.GOOGLE,
                "google-user-id"
        );
        verify(socialAccountRepository, never()).save(any(SocialAccount.class));
        verifyNoInteractions(memberQueryService, memberRegistrationService, passwordEncoder);
    }

    @Test
    void 소셜_계정이_없으면_멤버와_소셜_계정을_생성하고_멤버를_반환한다() {
        OAuth2UserProfile profile = profile();
        when(socialAccountRepository.findByProviderAndProviderUserId(
                profile.provider(),
                profile.providerUserId()
        )).thenReturn(Optional.empty());
        when(memberQueryService.existsByEmail(profile.email())).thenReturn(false);
        when(memberQueryService.existsByNickname(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encoded-password");
        when(memberRegistrationService.registerSocialMember(
                eq(profile.email()),
                anyString(),
                eq("encoded-password")
        )).thenAnswer(invocation -> member(profile.email(), invocation.getArgument(1)));

        Member result = socialAccountService.findOrCreateMember(profile);

        ArgumentCaptor<String> nicknameCaptor = ArgumentCaptor.forClass(String.class);
        verify(memberRegistrationService).registerSocialMember(
                eq(profile.email()),
                nicknameCaptor.capture(),
                eq("encoded-password")
        );
        assertThat(nicknameCaptor.getValue()).matches("google_[a-f0-9]{6}");
        assertThat(result.getEmail()).isEqualTo(profile.email());
        assertThat(result.getNickName()).isEqualTo(nicknameCaptor.getValue());

        ArgumentCaptor<SocialAccount> accountCaptor = ArgumentCaptor.forClass(SocialAccount.class);
        verify(socialAccountRepository).save(accountCaptor.capture());
        SocialAccount savedAccount = accountCaptor.getValue();
        assertThat(savedAccount.getMember()).isSameAs(result);
        assertThat(savedAccount.getProvider()).isEqualTo(profile.provider());
        assertThat(savedAccount.getProviderUserId()).isEqualTo(profile.providerUserId());

        ArgumentCaptor<String> passwordCaptor = ArgumentCaptor.forClass(String.class);
        verify(passwordEncoder).encode(passwordCaptor.capture());
        assertThat(passwordCaptor.getValue()).isNotBlank();
    }

    @Test
    void 신규_소셜_계정의_이메일이_이미_사용중이면_자동_연결하거나_저장하지_않는다() {
        OAuth2UserProfile profile = profile();
        when(socialAccountRepository.findByProviderAndProviderUserId(
                profile.provider(),
                profile.providerUserId()
        )).thenReturn(Optional.empty());
        when(memberQueryService.existsByEmail(profile.email())).thenReturn(true);

        assertThatThrownBy(() -> socialAccountService.findOrCreateMember(profile))
                .isInstanceOf(ExistingUserException.class)
                .hasMessageContaining("계정 연결이 필요합니다");
        verify(memberQueryService, never()).existsByNickname(anyString());
        verify(socialAccountRepository, never()).save(any(SocialAccount.class));
        verifyNoInteractions(memberRegistrationService, passwordEncoder);
    }

    @Test
    void 생성한_닉네임이_중복되면_새_닉네임을_생성한_후_멤버를_저장한다() {
        OAuth2UserProfile profile = profile();
        when(socialAccountRepository.findByProviderAndProviderUserId(
                profile.provider(),
                profile.providerUserId()
        )).thenReturn(Optional.empty());
        when(memberQueryService.existsByEmail(profile.email())).thenReturn(false);
        when(memberQueryService.existsByNickname(anyString())).thenReturn(true, false);
        when(passwordEncoder.encode(anyString())).thenReturn("encoded-password");
        when(memberRegistrationService.registerSocialMember(
                eq(profile.email()),
                anyString(),
                eq("encoded-password")
        )).thenAnswer(invocation -> member(profile.email(), invocation.getArgument(1)));

        Member result = socialAccountService.findOrCreateMember(profile);

        ArgumentCaptor<String> checkedNicknameCaptor = ArgumentCaptor.forClass(String.class);
        verify(memberQueryService, times(2)).existsByNickname(checkedNicknameCaptor.capture());
        assertThat(checkedNicknameCaptor.getAllValues())
                .hasSize(2)
                .allMatch(nickname -> nickname.matches("google_[a-f0-9]{6}"));
        assertThat(result.getNickName()).isEqualTo(checkedNicknameCaptor.getAllValues().get(1));
        verify(socialAccountRepository).save(any(SocialAccount.class));
    }

    @ParameterizedTest
    @MethodSource("invalidProfiles")
    void 필수_소셜_프로필이_없으면_조회와_저장을_시작하지_않는다(
            OAuth2UserProfile invalidProfile,
            String expectedMessage
    ) {
        assertThatThrownBy(() -> socialAccountService.findOrCreateMember(invalidProfile))
                .isInstanceOf(OAuth2AuthenticationException.class)
                .hasMessageContaining(expectedMessage);
        verifyNoInteractions(
                socialAccountRepository,
                memberQueryService,
                memberRegistrationService,
                passwordEncoder
        );
    }

    private static Stream<Arguments> invalidProfiles() {
        return Stream.of(
                Arguments.of(null, "소셜 사용자 정보가 비어있습니다"),
                Arguments.of(
                        new OAuth2UserProfile(null, "provider-id", "puppy@example.com", "puppy"),
                        "소셜 사용자 정보가 비어있습니다"
                ),
                Arguments.of(
                        new OAuth2UserProfile(SocialProvider.GOOGLE, " ", "puppy@example.com", "puppy"),
                        "소셜 사용자 식별자가 비어있습니다"
                ),
                Arguments.of(
                        new OAuth2UserProfile(SocialProvider.GOOGLE, "provider-id", " ", "puppy"),
                        "소셜 이메일 제공 동의가 필요합니다"
                )
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

    private Member member(String email, String nickname) {
        return Member.builder()
                .email(email)
                .nickName(nickname)
                .password("encoded-password")
                .build();
    }

    private SocialAccount socialAccount(OAuth2UserProfile profile, Member member) {
        return SocialAccount.builder()
                .member(member)
                .provider(profile.provider())
                .providerUserId(profile.providerUserId())
                .build();
    }
}
