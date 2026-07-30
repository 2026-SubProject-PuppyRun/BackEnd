package org.zerock.puppyrun.auth.oauth2.factory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.zerock.puppyrun.auth.oauth2.client.OAuth2ProviderStrategy;
import org.zerock.puppyrun.auth.oauth2.entity.SocialProvider;
import org.zerock.puppyrun.common.exception.InvalidValueException;

class OAuth2ClientFactoryTest {

    @Test
    void 공급자에_해당하는_전략을_반환한다() {
        OAuth2ProviderStrategy google = mock(OAuth2ProviderStrategy.class);
        OAuth2ProviderStrategy kakao = mock(OAuth2ProviderStrategy.class);
        when(google.getProvider()).thenReturn(SocialProvider.GOOGLE);
        when(kakao.getProvider()).thenReturn(SocialProvider.KAKAO);

        OAuth2ClientFactory factory = new OAuth2ClientFactory(List.of(google, kakao));

        assertThat(factory.getStrategy(SocialProvider.GOOGLE)).isSameAs(google);
        assertThat(factory.getStrategy(SocialProvider.KAKAO)).isSameAs(kakao);
    }

    @Test
    void 등록되지_않은_공급자는_거부한다() {
        OAuth2ProviderStrategy google = mock(OAuth2ProviderStrategy.class);
        when(google.getProvider()).thenReturn(SocialProvider.GOOGLE);
        OAuth2ClientFactory factory = new OAuth2ClientFactory(List.of(google));

        assertThatThrownBy(() -> factory.getStrategy(SocialProvider.NAVER))
                .isInstanceOf(InvalidValueException.class);
    }
}
