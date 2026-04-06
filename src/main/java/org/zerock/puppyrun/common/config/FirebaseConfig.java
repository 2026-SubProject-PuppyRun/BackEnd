package org.zerock.puppyrun.common.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import java.io.InputStream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.zerock.puppyrun.common.exception.BusinessException;
import org.zerock.puppyrun.common.exception.ErrorCode;

@Slf4j
@Configuration
public class FirebaseConfig {

    @Value("${firebase.account-path}")
    private String FIREBASE_ACCOUNT_PATH;

    @PostConstruct
    public void init() {
        try {
            log.info("Firebase 초기화 시작");
            InputStream serviceAccount = new ClassPathResource(FIREBASE_ACCOUNT_PATH).getInputStream();

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
