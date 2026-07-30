package org.zerock.puppyrun.member.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.zerock.puppyrun.member.entity.Member;
import org.zerock.puppyrun.member.repository.MemberRepository;

@ExtendWith(MockitoExtension.class)
class MemberRegistrationServiceTest {

    @Mock
    private MemberRepository memberRepository;

    private MemberRegistrationService memberRegistrationService;

    @BeforeEach
    void setUp() {
        memberRegistrationService = new MemberRegistrationService(memberRepository);
        when(memberRepository.save(any(Member.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void 소셜_회원_정보로_멤버를_생성하고_저장한다() {
        Member result = memberRegistrationService.registerSocialMember(
                "puppy@example.com",
                "google_a1b2c3",
                "encoded-password"
        );

        ArgumentCaptor<Member> memberCaptor = ArgumentCaptor.forClass(Member.class);
        verify(memberRepository).save(memberCaptor.capture());
        assertThat(result).isSameAs(memberCaptor.getValue());
        assertThat(result.getId()).isNotNull();
        assertThat(result.getEmail()).isEqualTo("puppy@example.com");
        assertThat(result.getNickName()).isEqualTo("google_a1b2c3");
        assertThat(result.getPassword()).isEqualTo("encoded-password");
    }

    @Test
    void 일반_회원_정보로_멤버를_생성하고_DTO를_반환한다() {
        var result = memberRegistrationService.registerLocalMember(
                "local@example.com",
                "local-user",
                "encoded-password"
        );

        assertThat(result.id()).isNotNull();
        assertThat(result.email()).isEqualTo("local@example.com");
        assertThat(result.nickName()).isEqualTo("local-user");
        verify(memberRepository).save(any(Member.class));
    }
}
