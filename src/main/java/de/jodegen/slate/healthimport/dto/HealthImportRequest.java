package de.jodegen.slate.healthimport.dto;

import java.util.List;

public record HealthImportRequest(
        List<StepSample> steps,
        List<SleepSample> sleep,
        List<WorkoutSample> workouts
) {}
