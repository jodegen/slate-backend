package de.jodegen.slate.healthimport.dto;

import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.List;

public record HealthImportRequest(
        @NotNull Instant sentAt,
        List<StepSample> steps,
        List<SleepSample> sleep,
        List<WorkoutSample> workouts
) {}
