package com.ticketch.userservice.application.port.in;

public interface RegisterUserUseCase {

    Long register(RegisterCommand command);

    record RegisterCommand(String email, String rawPassword, String name) {}
}
