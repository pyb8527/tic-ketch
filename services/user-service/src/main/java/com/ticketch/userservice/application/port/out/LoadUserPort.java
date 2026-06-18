package com.ticketch.userservice.application.port.out;

import com.ticketch.userservice.domain.model.User;

import java.util.Optional;

/**
 * [Output Port] 사용자 조회 인터페이스.
 *
 * <p>구현체: {@link com.ticketch.userservice.adapter.out.persistence.UserPersistenceAdapter}
 */
public interface LoadUserPort {

    /** 이메일로 사용자 조회 (로그인, 중복 체크에 사용) */
    Optional<User> findByEmail(String email);

    /** ID로 사용자 조회 (내 프로필 조회에 사용) */
    Optional<User> findById(Long userId);
}
