package com.edu.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 정적 리소스 라우팅 보강.
 *
 * 브라우저는 페이지를 열 때 루트 /favicon.ico 를 자동 요청한다.
 * favicon 파일은 static/img/ 아래에 두므로(= /img/favicon.ico), 루트 요청을 그리로 forward 해
 * "No static resource favicon.ico" WARN 로그를 없앤다.
 *
 * (@EnableWebMvc 미사용이라 Spring Boot 기본 정적 리소스 처리는 그대로 유지된다.)
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/favicon.ico").setViewName("forward:/img/favicon.ico");
    }
}
