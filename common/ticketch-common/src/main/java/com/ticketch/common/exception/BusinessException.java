package com.ticketch.common.exception;

import lombok.Getter;

/**
 * 비즈니스 규칙 위반 시 던지는 공통 예외.
 *
 * <p>모든 서비스의 애플리케이션/도메인 레이어에서 사용하며,
 * {@link GlobalExceptionHandler}가 HTTP 응답으로 변환한다.
 */
@Getter
public class BusinessException extends RuntimeException {

    /** 이 예외의 원인이 된 에러 코드 */
    private final ErrorCode errorCode;

    /**
     * 에러 코드에 정의된 기본 메시지로 예외 생성.
     *
     * @param errorCode 비즈니스 에러 코드
     */
    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    /**
     * 커스텀 메시지로 예외 생성 (상세 정보 포함 시 사용).
     *
     * @param errorCode 비즈니스 에러 코드
     * @param message   상세 메시지
     */
    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}
