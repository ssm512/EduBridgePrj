package com.edu.domain.notification.service;

import com.edu.domain.notification.mapper.DeviceTokenMapper;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * FCM 푸시 발송. FirebaseMessaging 빈이 없으면(=FCM 미설정) 조용히 아무것도 하지 않는다.
 * 알림 생성 흐름을 막지 않도록 @Async 로 비동기 발송하고, 무효 토큰은 자동 정리한다.
 */
@Service
public class FcmService {

    private static final Logger log = LoggerFactory.getLogger(FcmService.class);

    private final ObjectProvider<FirebaseMessaging> messagingProvider;
    private final DeviceTokenMapper deviceTokenMapper;

    public FcmService(ObjectProvider<FirebaseMessaging> messagingProvider,
                      DeviceTokenMapper deviceTokenMapper) {
        this.messagingProvider = messagingProvider;
        this.deviceTokenMapper = deviceTokenMapper;
    }

    /** 특정 사용자의 모든 기기로 푸시. type: ATTENDANCE/FEE/NOTICE (앱에서 분기용) */
    @Async
    public void sendToUser(Long userId, String type, String title, String body) {
        FirebaseMessaging fm = messagingProvider.getIfAvailable();
        if (fm == null || userId == null) {
            return; // FCM 비활성 - 인앱 알림만 저장되고 푸시는 생략
        }
        List<String> tokens = deviceTokenMapper.findTokensByUserId(userId);
        for (String token : tokens) {
            try {
                Message msg = Message.builder()
                        .setToken(token)
                        .setNotification(Notification.builder().setTitle(title).setBody(body).build())
                        .putData("type", type == null ? "" : type)
                        .build();
                fm.send(msg);
            } catch (FirebaseMessagingException e) {
                MessagingErrorCode code = e.getMessagingErrorCode();
                // 폐기/무효 토큰이면 DB에서 정리
                if (code == MessagingErrorCode.UNREGISTERED || code == MessagingErrorCode.INVALID_ARGUMENT) {
                    try { deviceTokenMapper.deleteByToken(token); } catch (Exception ignore) { }
                }
                log.debug("FCM 발송 실패 token={} : {}", token, e.getMessage());
            } catch (Exception e) {
                log.debug("FCM 발송 오류: {}", e.getMessage());
            }
        }
    }
}
