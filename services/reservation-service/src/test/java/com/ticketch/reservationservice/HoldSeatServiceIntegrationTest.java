package com.ticketch.reservationservice;

import com.ticketch.common.exception.BusinessException;
import com.ticketch.reservationservice.application.port.in.HoldSeatUseCase;
import com.ticketch.reservationservice.application.port.out.PublishSeatReleasedPort;
import com.ticketch.reservationservice.application.port.out.ValidateSeatPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

/**
 * [통합 테스트] HoldSeatService — 동시 좌석 선점 시나리오 검증.
 *
 * <p>이 테스트는 Redisson 분산 락({@code AcquireSeatLockPort})이 동시에 같은 좌석을
 * 선점하려는 N개의 스레드 중 정확히 1건만 성공하고 나머지는 {@link BusinessException}
 * ({@code SEAT_ALREADY_HELD} 또는 {@code LOCK_ACQUISITION_FAILED})을 받도록
 * 보장함을 검증한다.
 *
 * <ul>
 *   <li>MySQL 8 Testcontainer: Flyway {@code V1__create_reservations.sql}이 실행되어
 *       실제 DB 스키마에서 검증한다.</li>
 *   <li>Redis 7 Testcontainer: Redisson 분산 락 및 예약 캐시에 사용된다.</li>
 *   <li>RabbitMQ / Feign 연동은 {@code @MockBean}으로 대체하여 외부 인프라 의존성을 제거한다.</li>
 * </ul>
 *
 * <p>Docker가 실행 중이지 않으면 이 테스트는 컨테이너 시작에 실패하므로,
 * Docker 기동 후 실행해야 한다.
 */
@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class HoldSeatServiceIntegrationTest {

    // -----------------------------------------------------------------------
    // Testcontainers — static so containers are shared across all test methods
    // -----------------------------------------------------------------------

    @Container
    static final MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8")
            .withDatabaseName("ticketch_reservation_test")
            .withUsername("test")
            .withPassword("test");

    @SuppressWarnings("resource")
    @Container
    static final GenericContainer<?> redis =
            new GenericContainer<>(DockerImageName.parse("redis:7"))
                    .withExposedPorts(6379);

    // -----------------------------------------------------------------------
    // Dynamic properties — override application-test.yml values
    // -----------------------------------------------------------------------

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        // MySQL datasource
        r.add("spring.datasource.url",      mysql::getJdbcUrl);
        r.add("spring.datasource.username", mysql::getUsername);
        r.add("spring.datasource.password", mysql::getPassword);
        r.add("spring.datasource.driver-class-name", () -> "com.mysql.cj.jdbc.Driver");

        // Let Flyway create schema on the real MySQL container;
        // disable JPA auto-ddl so Flyway is the sole schema manager.
        r.add("spring.jpa.hibernate.ddl-auto", () -> "none");
        r.add("spring.flyway.enabled",           () -> "true");

        // Redis
        r.add("spring.data.redis.host", redis::getHost);
        r.add("spring.data.redis.port", () -> redis.getMappedPort(6379).toString());

        // Prevent @RabbitListener from connecting — no RabbitMQ container is started
        r.add("spring.rabbitmq.listener.simple.auto-startup", () -> "false");
    }

    // -----------------------------------------------------------------------
    // Mocked external dependencies
    // -----------------------------------------------------------------------

    /** RabbitMQ 이벤트 발행 포트 — 외부 MQ 없이 Mock으로 대체 */
    @MockBean
    PublishSeatReleasedPort publishSeatReleasedPort;

    /** Feign(event-service) 좌석 검증 포트 — 항상 AVAILABLE 반환하도록 Stub */
    @MockBean
    ValidateSeatPort validateSeatPort;

    // -----------------------------------------------------------------------
    // SUT
    // -----------------------------------------------------------------------

    @Autowired
    HoldSeatUseCase holdSeatUseCase;

    // -----------------------------------------------------------------------
    // Setup
    // -----------------------------------------------------------------------

    @BeforeEach
    void stubValidateSeat() {
        // 모든 (eventId, seatId) 조합에 대해 좌석이 AVAILABLE 하다고 가정
        when(validateSeatPort.isAvailable(anyLong(), anyLong())).thenReturn(true);
    }

    // -----------------------------------------------------------------------
    // Test
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("동시에 같은 좌석 선점 시 한 건만 성공하고 나머지 19건은 BusinessException이어야 한다")
    void 동시에_같은_좌석_선점시_한건만_성공() throws InterruptedException {
        final int threadCount = 20;
        final long seatId    = 100L;
        final long eventId   = 1L;

        ExecutorService executor    = Executors.newFixedThreadPool(threadCount);
        CountDownLatch  readyLatch  = new CountDownLatch(threadCount); // 모든 스레드가 준비될 때까지 대기
        CountDownLatch  startLatch  = new CountDownLatch(1);           // 동시 출발 신호
        CountDownLatch  doneLatch   = new CountDownLatch(threadCount); // 모든 스레드 완료 대기

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount    = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            final long userId = (long) (i + 1); // userId: 1 ~ 20 (고유)
            executor.submit(() -> {
                try {
                    readyLatch.countDown();   // 준비 완료 신호
                    startLatch.await();       // 출발 신호 대기 — 모든 스레드가 동시에 출발

                    holdSeatUseCase.holdSeat(
                            new HoldSeatUseCase.HoldSeatCommand(userId, seatId, eventId));

                    successCount.incrementAndGet();
                } catch (BusinessException e) {
                    failCount.incrementAndGet();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        // 모든 스레드가 준비될 때까지 기다린 뒤 동시 출발
        readyLatch.await();
        startLatch.countDown();

        // 모든 스레드 완료 대기
        doneLatch.await();
        executor.shutdown();

        // 정확히 1건 성공, 19건 실패
        assertThat(successCount.get())
                .as("Redisson 분산 락으로 인해 단 한 스레드만 좌석을 선점해야 한다")
                .isEqualTo(1);

        assertThat(failCount.get())
                .as("나머지 19개 스레드는 BusinessException(SEAT_ALREADY_HELD 또는 LOCK_ACQUISITION_FAILED)을 받아야 한다")
                .isEqualTo(19);
    }
}
