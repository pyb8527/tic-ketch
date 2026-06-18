package com.ticketch.userservice.application.port.in;

public interface LogoutUseCase {

    void logout(String accessToken, String refreshToken);
}
