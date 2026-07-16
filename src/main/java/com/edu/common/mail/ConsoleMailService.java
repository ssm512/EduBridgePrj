package com.edu.common.mail;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * [개발용] 실제 메일 발송 대신 콘솔(IntelliJ Run 창)에 내용을 출력한다.
 *
 * ⚠️ TODO-DELETE-BEFORE-DEPLOY: 배포 전 삭제 대상.
 * app.mail.enabled=true (+ spring.mail.* 설정)면 SmtpMailService가 자동으로 이 클래스를
 * 대체하므로(@ConditionalOnProperty) 동작상으로는 지금 당장 지우지 않아도 안전하지만,
 * 운영 배포 전 정리 차원에서 이 파일과 아래 @ConditionalOnProperty 두 줄을 삭제하고
 * SmtpMailService를 MailService의 유일한 구현체로 남기면 된다.
 */
@Service
@ConditionalOnProperty(prefix = "app.mail", name = "enabled", havingValue = "false", matchIfMissing = true)
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
