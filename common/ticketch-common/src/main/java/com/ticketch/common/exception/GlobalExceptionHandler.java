package com.ticketch.common.exception;

import com.ticketch.common.response.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

/**
 * 전체 서비스 공통 예외 처리기.
 *
 * <p>ticketch-common 모듈에 위치하며, 각 서비스의 컴포넌트 스캔 범위에
 * {@code com.ticketch} 패키지가 포함되면 자동으로 등록된다.
 *
 * <p>처리 순서:
 * <ol>
 *   <li>{@link BusinessException} — 비즈니스 규칙 위반</li>
 *   <li>{@link MethodArgumentNotValidException} — @Valid 검증 실패</li>
 *   <li>{@link BindException} — @ModelAttribute 바인딩 실패</li>
 *   <li>{@link HttpMessageNotReadableException} — JSON 파싱 실패</li>
 *   <li>{@link Exception} — 그 외 모든 예외 (500)</li>
 * </ol>
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 비즈니스 규칙 위반 처리.
     * ErrorCode에 정의된 HTTP 상태코드로 응답한다.
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException e) {
        log.warn("BusinessException: {}", e.getMessage());
        ErrorCode errorCode = e.getErrorCode();
        return ResponseEntity
                .status(errorCode.getStatus())
                .body(ErrorResponse.of(errorCode));
    }

    /**
     * @Valid, @Validated 검증 실패 처리.
     * 각 필드의 오류 메시지를 FieldError 목록으로 반환한다.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException e) {
        List<ErrorResponse.FieldError> fieldErrors = e.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(fe -> ErrorResponse.FieldError.builder()
                        .field(fe.getField())
                        .value(fe.getRejectedValue() == null ? "" : fe.getRejectedValue().toString())
                        .reason(fe.getDefaultMessage())
                        .build())
                .toList();

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of(ErrorCode.INVALID_INPUT, fieldErrors));
    }

    /** @ModelAttribute 바인딩 실패 처리 */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<ErrorResponse> handleBindException(BindException e) {
        List<ErrorResponse.FieldError> fieldErrors = e.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(fe -> ErrorResponse.FieldError.builder()
                        .field(fe.getField())
                        .value(fe.getRejectedValue() == null ? "" : fe.getRejectedValue().toString())
                        .reason(fe.getDefaultMessage())
                        .build())
                .toList();

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of(ErrorCode.INVALID_INPUT, fieldErrors));
    }

    /** 요청 바디 JSON 파싱 실패 처리 (잘못된 형식, 누락된 필드 등) */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadable(HttpMessageNotReadableException e) {
        log.warn("HttpMessageNotReadableException: {}", e.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of(ErrorCode.INVALID_INPUT));
    }

    /** 처리되지 않은 모든 예외 — 500 Internal Server Error */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception e) {
        log.error("Unhandled exception", e);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.of(ErrorCode.INTERNAL_SERVER_ERROR));
    }
}
