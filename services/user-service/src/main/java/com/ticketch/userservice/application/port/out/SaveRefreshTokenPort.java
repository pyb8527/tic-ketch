package com.ticketch.userservice.application.port.out;

public interface SaveRefreshTokenPort {

    void save(Long userId, String refreshToken);

    void revoke(Long userId);
}
