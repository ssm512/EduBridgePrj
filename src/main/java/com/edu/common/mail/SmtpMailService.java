package com.edu.common.mail;

import com.edu.common.exception.ApiException;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

/**
 * [추가 2026-07-20] 운영용 메일 발송 구현체 (spring-boot-starter-mail / SMTP).
 *
 * app.mail.enabled=true일 때만 활성화되며, 이때 ConsoleMailService는 자동으로 비활성화된다
 * (둘 다 @ConditionalOnProperty로 상호 배타적 - MailService 빈이 항상 정확히 하나만 존재).
 *
 * 발송 실패 시 예외를 그대로 던진다. AuthService가 클래스 레벨 @Transactional이라
 * 여기서 예외가 나면 방금 처리한 비밀번호 초기화(userMapper.resetPassword)와
 * 리프레시 토큰 폐기가 함께 롤백되어, "메일은 안 갔는데 비밀번호만 바뀌어서
 * 로그인 못 하는" 잠금 상태를 막아준다.
 */
@Service
@ConditionalOnProperty(prefix = "app.mail", name = "enabled", havingValue = "true")
public class SmtpMailService implements MailService {

    private final JavaMailSender mailSender;
    private final String fromAddress;

    public SmtpMailService(JavaMailSender mailSender, @Value("${app.mail.from}") String fromAddress) {
        this.mailSender = mailSender;
        this.fromAddress = fromAddress;
    }

    @Override
    public void sendTempPassword(String to, String name, String tempPassword) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
            helper.setFrom(fromAddress);
            helper.setTo(to);
            helper.setSubject("[EduBridge] 임시 비밀번호 안내");
            helper.setText("""
                    %s님, 안녕하세요.

                    요청하신 임시 비밀번호가 발급되었습니다.

                    임시 비밀번호: %s

                    로그인 후 반드시 새 비밀번호로 변경해 주세요.
                    본인이 요청하지 않았다면 이 메일을 무시하셔도 됩니다.
                    """.formatted(name, tempPassword));

            mailSender.send(message);
        } catch (MessagingException e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "메일 발송에 실패했습니다");
        } catch (MailException e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "메일 발송에 실패했습니다");
        }
    }
}
