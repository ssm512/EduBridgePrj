package com.edu;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling   // RefreshTokenService의 만료 토큰 삭제 배치(@Scheduled) 활성화
public class EduBridgePrjApplication {

    public static void main(String[] args) {
        SpringApplication.run(EduBridgePrjApplication.class, args);
    }
}
