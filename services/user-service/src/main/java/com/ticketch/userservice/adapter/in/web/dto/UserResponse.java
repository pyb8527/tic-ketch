package com.ticketch.userservice.adapter.in.web.dto;

import com.ticketch.userservice.domain.model.User;

import java.time.LocalDateTime;

public record UserResponse(Long id, String email, String name, String role, LocalDateTime createdAt) {

    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getRole().name(),
                user.getCreatedAt()
        );
    }
}
