-- 공연 테이블
CREATE TABLE events (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    title       VARCHAR(255) NOT NULL,
    venue       VARCHAR(255) NOT NULL,
    event_date  DATETIME     NOT NULL,
    status      VARCHAR(20)  NOT NULL DEFAULT 'UPCOMING',
    created_at  DATETIME     NOT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 좌석 등급 테이블
CREATE TABLE seat_grades (
    id          BIGINT      NOT NULL AUTO_INCREMENT,
    event_id    BIGINT      NOT NULL,
    grade_name  VARCHAR(50) NOT NULL,
    price       INT         NOT NULL,
    color_code  VARCHAR(10),
    PRIMARY KEY (id),
    FOREIGN KEY (event_id) REFERENCES events(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 좌석 테이블 (version: JPA Optimistic Lock)
CREATE TABLE seats (
    id            BIGINT      NOT NULL AUTO_INCREMENT,
    event_id      BIGINT      NOT NULL,
    seat_grade_id BIGINT      NOT NULL,
    row_name      VARCHAR(10) NOT NULL,
    seat_number   INT         NOT NULL,
    status        VARCHAR(15) NOT NULL DEFAULT 'AVAILABLE',
    version       BIGINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    FOREIGN KEY (event_id)      REFERENCES events(id),
    FOREIGN KEY (seat_grade_id) REFERENCES seat_grades(id),
    INDEX idx_seats_event_id (event_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
