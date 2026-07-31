package com.edu.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.FileInputStream;
import java.io.InputStream;

/**
 * FCM(Firebase Admin) 초기화.
 * app.fcm.enabled=true 이고 서비스계정 JSON 경로(app.fcm.credentials-path)가 있을 때만 FirebaseMessaging 빈 생성.
 * 미설정(기본)이면 빈이 없고, FcmService가 이를 감지해 푸시를 조용히 건너뛴다 → 팀 빌드/부팅에 영향 없음.
 */
@Configuration
public class FirebaseConfig {

    private static final Logger log = LoggerFactory.getLogger(FirebaseConfig.class);

    @Value("${app.fcm.credentials-path:}")
    private String credentialsPath;

    @Bean
    @ConditionalOnProperty(name = "app.fcm.enabled", havingValue = "true")
    public FirebaseMessaging firebaseMessaging() {
        try {
            if (credentialsPath == null || credentialsPath.isBlank()) {
                log.warn("FCM 활성화됐지만 app.fcm.credentials-path 가 비어 있어 푸시를 비활성화합니다.");
                return null;
            }
            try (InputStream sa = new FileInputStream(credentialsPath)) {
                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.fromStream(sa))
                        .build();
                FirebaseApp app = FirebaseApp.getApps().isEmpty()
                        ? FirebaseApp.initializeApp(options)
                        : FirebaseApp.getInstance();
                log.info("FCM(Firebase Admin) 초기화 완료");
                return FirebaseMessaging.getInstance(app);
            }
        } catch (Exception e) {
            // 키 파일이 없거나 잘못돼도 서버는 정상 부팅하고 푸시만 비활성
            log.warn("FCM 초기화 실패 - 푸시 비활성: {}", e.getMessage());
            return null;
        }
    }
}
