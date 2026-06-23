package com.ticketch.reservationservice.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Redisson 분산 락 클라이언트 설정.
 * Seat 선점을 위한 동시성 제어 및 분산 락을 제공하는 RedissonClient를 생성합니다.
 */
@Configuration
public class RedissonConfig {

    /**
     * Redisson 클라이언트 빈 생성.
     * Spring Data Redis 설정으로부터 호스트와 포트를 주입받아 단일 서버 모드로 초기화합니다.
     *
     * @param host Redis 호스트 주소 (기본값: localhost)
     * @param port Redis 포트 번호 (기본값: 6379)
     * @return 설정된 RedissonClient 인스턴스
     */
    @Bean(destroyMethod = "shutdown")
    public RedissonClient redissonClient(
            @Value("${spring.data.redis.host:localhost}") String host,
            @Value("${spring.data.redis.port:6379}") int port) {
        Config config = new Config();
        config.useSingleServer().setAddress("redis://" + host + ":" + port);
        return Redisson.create(config);
    }
}
