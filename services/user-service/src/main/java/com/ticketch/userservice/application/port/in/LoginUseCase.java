package com.ticketch.userservice.application.port.in;

public interface LoginUseCase {

    TokenPair login(LoginCommand command);

    record LoginCommand(String email, String rawPassword) {}
}
