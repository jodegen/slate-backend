package de.jodegen.slate.healthimport.dto;

import jakarta.validation.constraints.NotNull;

import java.time.OffsetDateTime;

public record SleepSample(
        @NotNull int sleepStage,
        @NotNull OffsetDateTime startDate,
        @NotNull OffsetDateTime endDate,
        @NotNull long durationSeconds
) {}
