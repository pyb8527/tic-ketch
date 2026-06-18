package com.ticketch.common.response;

import com.ticketch.common.exception.ErrorCode;
import lombok.Getter;

/**
 * 전체 서비스 공통 API 응답 래퍼.
 *
 * <p>모든 컨트롤러 응답은 이 객체로 감싸서 반환한다.
 * <pre>
 * 성공: { "code": "SUCCESS", "message": "ok", "data": { ... } }
 * 실패: { "code": "U001",    "message": "사용자를 찾을 수 없습니다", "data": null }
 * </pre>
 *
 * @param <T> 응답 데이터 타입
 */
@Getter
public class ApiResponse<T> {

    private final String code;
    private final String message;
    private final T data;

    private ApiResponse(String code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    /** 데이터 포함 성공 응답 */
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>("SUCCESS", "ok", data);
    }

    /** 데이터 없는 성공 응답 (삭제, 로그아웃 등) */
    public static <T> ApiResponse<T> ok() {
        return new ApiResponse<>("SUCCESS", "ok", null);
    }

    /** 에러 코드 기반 실패 응답 */
    public static <T> ApiResponse<T> error(ErrorCode errorCode) {
        return new ApiResponse<>(errorCode.getCode(), errorCode.getMessage(), null);
    }

    /** 커스텀 메시지 포함 실패 응답 */
    public static <T> ApiResponse<T> error(ErrorCode errorCode, String message) {
        return new ApiResponse<>(errorCode.getCode(), message, null);
    }
}
