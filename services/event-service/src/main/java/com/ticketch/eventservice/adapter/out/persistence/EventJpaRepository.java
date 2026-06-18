package com.ticketch.eventservice.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface EventJpaRepository extends JpaRepository<EventJpaEntity, Long> {
}
