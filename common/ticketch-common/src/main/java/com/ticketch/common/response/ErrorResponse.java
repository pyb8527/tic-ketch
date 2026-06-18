package com.ticketch.common.response;

import com.ticketch.common.exception.ErrorCode;
import lombok.Builder;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

/**
 * 유효성 검사 실패 등 상세 에러 정보가 필요한 경우의 응답 객체.
 *
 * <p>{@link com.ticketch.common.exception.GlobalExceptionHandler}에서 생성되며,
 * {@code errors} 필드에 필드별 오류 목록을 담는다.
 */
@Getter
@Builder
public class ErrorResponse {

    private final String code;
    private final String message;

    /** 필드 단위 유효성 오류 목록 (MethodArgumentNotValidException 등에서 채워짐) */
    @Builder.Default
    private final List<FieldError> errors = new ArrayList<>();

    /** 에러 코드만으로 응답 생성 (errors 빈 리스트) */
    public static ErrorResponse of(ErrorCode errorCode) {
        return ErrorResponse.builder()
                .code(errorCode.getCode())
                .message(errorCode.getMessage())
                .build();
    }

    /** 필드 에러 목록 포함 응답 생성 */
    public static ErrorResponse of(ErrorCode errorCode, List<FieldError> fieldErrors) {
        return ErrorResponse.builder()
                .code(errorCode.getCode())
                .message(errorCode.getMessage())
                .errors(fieldErrors)
                .build();
    }

    /**
     * 단일 필드 에러 정보.
     *
     * <p>Spring Validation의 {@code FieldError}를 직렬화하기 위한 내부 클래스.
     */
    @Getter
    @Builder
    public static class FieldError {
        /** 유효성 오류가 발생한 필드명 */
        private final String field;
        /** 거부된 값 */
        private final String value;
        /** 오류 이유 (제약 메시지) */
        private final String reason;
    }
}
