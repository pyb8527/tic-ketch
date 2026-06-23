package com.ticketch.eventservice.adapter.in.web.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

/** 공연 생성 요청 DTO (관리자 전용) */
public record CreateEventRequest(
        @NotBlank String title,
        @NotBlank String venue,
        String category,
        String posterUrl,
        @NotNull @Future LocalDateTime eventDate
) {}
