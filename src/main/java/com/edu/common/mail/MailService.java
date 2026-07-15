package com.edu.common.mail;

/**
 * 메일 발송 서비스.
 * 개발 단계에서는 ConsoleMailService(콘솔 출력)가 사용되고,
 * 운영 전환 시 SMTP 구현체(spring-boot-starter-mail)로 교체한다.
 */
public interface MailService {

    /**
     * 임시 비밀번호 안내 메일 발송
     *
     * @param to           수신자 이메일
     * @param name         수신자 이름
     * @param tempPassword 임시 비밀번호 (원문)
     */
    void sendTempPassword(String to, String name, String tempPassword);
}
