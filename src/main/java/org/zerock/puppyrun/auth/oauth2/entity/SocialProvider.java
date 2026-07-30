package org.zerock.puppyrun.auth.oauth2.entity;

import java.util.Arrays;
import java.util.Locale;
import org.zerock.puppyrun.common.exception.InvalidValueException;

/**
 * 지원하는 소셜 로그인 제공자입니다.
 */
public enum SocialProvider {
    GOOGLE,
    KAKAO,
    NAVER;

    public static SocialProvider from(String social) {
        if (social == null || social.isBlank()) {
            throw new InvalidValueException("소셜 로그인 제공자는 필수입니다.");
        }

        String normalizedSocial = social.trim().toUpperCase(Locale.ROOT);
        return Arrays.stream(values())
                .filter(provider -> provider.name().equals(normalizedSocial))
                .findFirst()
                .orElseThrow(() -> new InvalidValueException("지원하지 않는 소셜 로그인 제공자입니다: " + social));
    }
}
