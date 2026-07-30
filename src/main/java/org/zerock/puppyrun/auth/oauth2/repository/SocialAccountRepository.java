package org.zerock.puppyrun.auth.oauth2.repository;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.zerock.puppyrun.auth.oauth2.entity.SocialAccount;
import org.zerock.puppyrun.auth.oauth2.entity.SocialProvider;

@Repository
public interface SocialAccountRepository extends JpaRepository<SocialAccount, UUID> {
    Optional<SocialAccount> findByProviderAndProviderUserId(
            SocialProvider provider,
            String providerUserId
    );
}
