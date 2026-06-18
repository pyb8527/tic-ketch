package com.ticketch.userservice.application.port.out;

import com.ticketch.userservice.domain.model.User;

import java.util.Optional;

public interface LoadUserPort {

    Optional<User> findByEmail(String email);

    Optional<User> findById(Long userId);
}
