package de.jodegen.slate.healthimport.dto;

import jakarta.validation.constraints.NotNull;

import java.time.OffsetDateTime;

public record WorkoutSample(
        @NotNull String workoutType,
        @NotNull long durationSeconds,
        @NotNull OffsetDateTime startDate,
        @NotNull OffsetDateTime endDate
) {}
