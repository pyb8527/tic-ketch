package com.ticketch.userservice.application.port.out;

import java.util.Optional;

public interface LoadRefreshTokenPort {

    Optional<String> findByUserId(Long userId);
}
