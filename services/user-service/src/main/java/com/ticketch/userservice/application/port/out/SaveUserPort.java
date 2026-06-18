package com.ticketch.userservice.application.port.out;

import com.ticketch.userservice.domain.model.User;

/**
 * [Output Port] 사용자 저장 인터페이스.
 *
 * <p>구현체: {@link com.ticketch.userservice.adapter.out.persistence.UserPersistenceAdapter}
 */
public interface SaveUserPort {

    /**
     * 사용자를 영속화하고 생성된 ID를 반환한다.
     *
     * @param user 저장할 도메인 객체 (id는 null)
     * @return 생성된 사용자 ID
     */
    Long save(User user);
}
