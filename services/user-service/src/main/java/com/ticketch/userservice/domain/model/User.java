package com.ticketch.userservice.domain.model;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * [Domain] 사용자 도메인 모델.
 *
 * <p>헥사고날 아키텍처의 핵심 도메인 객체로, Spring · JPA 등 인프라 의존성이 없다.
 * JPA Entity({@link com.ticketch.userservice.adapter.out.persistence.UserJpaEntity})와
 * 별도로 관리하여 도메인 로직이 영속성 기술에 종속되지 않도록 한다.
 */
@Getter
@Builder
public class User {

    private Long id;
    private String email;
    /** BCrypt 인코딩된 비밀번호 */
    private String password;
    private String name;
    private UserRole role;
    private LocalDateTime createdAt;

    /** 사용자 권한 */
    public enum UserRole {
        USER,   // 일반 사용자
        ADMIN   // 관리자 (공연 등록, 통계 조회 가능)
    }
}
