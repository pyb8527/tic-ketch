-- 결제 정보 테이블
CREATE TABLE payments (
    id               BIGINT      NOT NULL AUTO_INCREMENT,
    reservation_id   BIGINT      NOT NULL UNIQUE,
    user_id          BIGINT      NOT NULL,
    amount           INT         NOT NULL,
    status           VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    failure_reason   VARCHAR(500),
    paid_at          DATETIME,
    created_at       DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX idx_payments_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
