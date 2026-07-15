package com.edu.common.mail;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * [개발용] 실제 메일 발송 대신 콘솔(IntelliJ Run 창)에 내용을 출력한다.
 * 운영 전환 시 SMTP 구현체를 만들고 이 클래스를 대체하거나
 * @Profile / @ConditionalOnProperty로 전환하면 된다.
 */
@Service
public class ConsoleMailService implements MailService {

    private static final Logger log = LoggerFactory.getLogger(ConsoleMailService.class);

    @Override
    public void sendTempPassword(String to, String name, String tempPassword) {
        log.info("""

                ========== [DEV] 임시 비밀번호 메일 (실제 발송 안 함) ==========
                수신자       : {} ({})
                임시 비밀번호 : {}
                안내         : 임시 비밀번호로 로그인하면 비밀번호 변경 화면으로 이동합니다.
                ================================================================
                """, to, name, tempPassword);
    }
}
