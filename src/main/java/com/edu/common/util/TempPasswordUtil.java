package com.edu.common.util;

import java.security.SecureRandom;

/**
 * 임시 비밀번호 생성 유틸.
 * - 관리자 초기화(A안)와 셀프 재설정(로그인 화면) 양쪽에서 공용 사용
 * - 영문 + 숫자, 혼동되기 쉬운 문자(0/O, 1/l/I) 제외
 */
public final class TempPasswordUtil {

    private static final String CHARS =
            "ABCDEFGHJKMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789";
    private static final int LENGTH = 10;
    private static final SecureRandom RANDOM = new SecureRandom();

    private TempPasswordUtil() {
    }

    /** 영문+숫자 10자 임시 비밀번호 생성 */
    public static String create() {
        StringBuilder sb = new StringBuilder(LENGTH);
        for (int i = 0; i < LENGTH; i++) {
            sb.append(CHARS.charAt(RANDOM.nextInt(CHARS.length())));
        }
        return sb.toString();
    }
}
