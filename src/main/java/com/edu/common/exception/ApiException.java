package com.edu.common.exception;

import org.springframework.http.HttpStatus;

public class ApiException extends RuntimeException {

    private final HttpStatus status;

    public ApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    /** 원인 예외를 함께 남기고 싶을 때 사용 (예: 외부 API 호출 실패) - 2026-07-21 AIG-02에서 추가 */
    public ApiException(HttpStatus status, String message, Throwable cause) {
        super(message, cause);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }

}
