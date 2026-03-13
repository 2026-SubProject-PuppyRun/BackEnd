package org.zerock.puppyrun.common.init;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.zerock.puppyrun.common.auth.jwt.JwtTokenProvider;
import org.zerock.puppyrun.member.entity.Member;
import org.zerock.puppyrun.member.repository.MemberRepository;

@Component
@RequiredArgsConstructor
@Slf4j
@Profile("local")
public class DataInitializer implements CommandLineRunner {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    private static final String ADMIN_EMAIL = "admin@puppyrun.com";
    private static final String ADMIN_PASSWORD = "admin1234";
    private static final String ADMIN_NICKNAME = "admin";

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        log.info("데이터 초기화를 시작합니다...");

        // 관리자 계정이 없으면 생성하고, 있으면 기존 계정을 사용합니다.
        Member admin = memberRepository.findByEmail(ADMIN_EMAIL)
                .orElseGet(this::createAdminUser);

        // 확보된 관리자 계정으로 토큰을 발급하고 로그를 출력합니다.
        String accessToken = jwtTokenProvider.generateAccessToken(admin.toDto());
        log.info("관리자 계정({})으로 토큰을 발급했습니다.", admin.getEmail());
        log.info("Admin Access Token: {}", accessToken);

        log.info("데이터 초기화가 완료되었습니다.");
    }

    private Member createAdminUser() {
        log.info("관리자 계정이 존재하지 않아 새로 생성합니다.");
        Member admin = Member.builder()
                .email(ADMIN_EMAIL)
                .nickName(ADMIN_NICKNAME)
                .password(passwordEncoder.encode(ADMIN_PASSWORD))
                .build();
        admin.setAdmin();
        return memberRepository.save(admin);
    }
}
