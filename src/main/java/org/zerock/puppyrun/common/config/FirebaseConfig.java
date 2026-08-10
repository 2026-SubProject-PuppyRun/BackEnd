package org.zerock.puppyrun.common.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import java.io.InputStream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.zerock.puppyrun.common.exception.BusinessException;
import org.zerock.puppyrun.common.exception.ErrorCode;

@Slf4j
@Configuration
@Profile("!test")
public class FirebaseConfig {

    private final ResourceLoader resourceLoader;
    private final String FIREBASE_ACCOUNT_PATH;

    public FirebaseConfig(
            ResourceLoader resourceLoader,
            @Value("${firebase.account-path}") String firebase

    ) {
        this.FIREBASE_ACCOUNT_PATH = firebase;
        this.resourceLoader = resourceLoader;
    }

    @PostConstruct
    public void init() {
        String resourceLocation =
                FIREBASE_ACCOUNT_PATH.startsWith("classpath:") || FIREBASE_ACCOUNT_PATH.startsWith("file:")
                        ? FIREBASE_ACCOUNT_PATH
                        : "classpath:" + FIREBASE_ACCOUNT_PATH;
        Resource serviceAccountResource = resourceLoader.getResource(resourceLocation);

        try (InputStream serviceAccount = serviceAccountResource.getInputStream()) {
            log.info("Firebase 초기화 시작");

            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .build();

            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(options);
            }
            log.info("Firebase 초기화 완료");
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "Firebase 초기화 실패", e);
        }
    }
}
