package com.edu.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * 비동기 실행 활성화. FCM 발송(FcmService.sendToUser)을 @Async로 돌려
 * 알림 생성/출석 처리 요청 스레드를 막지 않도록 한다.
 */
@Configuration
@EnableAsync
public class AsyncConfig {
}
