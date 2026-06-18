package com.ticketch.userservice.adapter.out.persistence;

import com.ticketch.userservice.domain.model.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * [Persistence Adapter - JPA Entity] users 테이블 매핑 객체.
 *
 * <p>헥사고날 원칙에 따라 도메인 모델({@link User})과 분리된다.
 * 상호 변환은 {@link #toDomain()}과 {@link #fromDomain(User)}를 통해 수행한다.
 * <p>Flyway 마이그레이션: V1__create_users.sql
 */
@Entity
@Table(name = "users")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    /** BCrypt 인코딩된 비밀번호 */
    @Column(nullable = false)
    private String password;

    @Column(nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private User.UserRole role;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    /** JPA Entity → 도메인 모델 변환 */
    public User toDomain() {
        return User.builder()
                .id(id).email(email).password(password)
                .name(name).role(role).createdAt(createdAt)
                .build();
    }

    /** 도메인 모델 → JPA Entity 변환 */
    public static UserJpaEntity fromDomain(User user) {
        return UserJpaEntity.builder()
                .id(user.getId()).email(user.getEmail()).password(user.getPassword())
                .name(user.getName()).role(user.getRole()).createdAt(user.getCreatedAt())
                .build();
    }
}
