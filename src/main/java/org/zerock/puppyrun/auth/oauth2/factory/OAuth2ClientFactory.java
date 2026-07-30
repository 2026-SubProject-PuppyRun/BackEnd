package org.zerock.puppyrun.auth.oauth2.factory;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import org.zerock.puppyrun.auth.oauth2.client.OAuth2ProviderStrategy;
import org.zerock.puppyrun.auth.oauth2.entity.SocialProvider;
import org.zerock.puppyrun.common.exception.InvalidValueException;

/**
 * 소셜 로그인 제공자별 OAuth 클라이언트 공급자입니다.
 */
@Component
public class OAuth2ClientFactory {
    private final Map<SocialProvider, OAuth2ProviderStrategy> strategies;

    public OAuth2ClientFactory(List<OAuth2ProviderStrategy> strategyList) {
        this.strategies = strategyList.stream()
                .collect(Collectors.toUnmodifiableMap(
                        OAuth2ProviderStrategy::getProvider,
                        Function.identity()
                ));
    }

    public OAuth2ProviderStrategy getStrategy(SocialProvider provider) {
        OAuth2ProviderStrategy strategy = strategies.get(provider);
        if (strategy == null) {
            throw new InvalidValueException("지원하지 않는 소셜 로그인입니다: " + provider);
        }
        return strategy;

    }
}
