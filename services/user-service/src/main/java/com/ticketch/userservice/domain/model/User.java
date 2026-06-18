package com.ticketch.userservice.domain.model;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class User {

    private Long id;
    private String email;
    private String password;
    private String name;
    private UserRole role;
    private LocalDateTime createdAt;

    public enum UserRole {
        USER, ADMIN
    }
}
