package com.ticketch.notificationservice.adapter.out.persistence;

import org.springframework.data.mongodb.repository.MongoRepository;

/**
 * 알림 MongoDB 리포지토리.
 *
 * Spring Data MongoDB를 통해 NotificationDocument의 기본적인 CRUD 작업을 수행한다.
 */
public interface NotificationMongoRepository extends MongoRepository<NotificationDocument, String> {
}
