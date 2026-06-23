-- 예약 테이블 (임시 선점 관리)
CREATE TABLE reservations (
    id           BIGINT      NOT NULL AUTO_INCREMENT,
    user_id      BIGINT      NOT NULL,
    seat_id      BIGINT      NOT NULL,
    event_id     BIGINT      NOT NULL,
    status       VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    expires_at   DATETIME    NOT NULL,
    created_at   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX idx_reservations_seat_id_status (seat_id, status),
    INDEX idx_reservations_user_id (user_id),
    INDEX idx_reservations_status_expires_at (status, expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
