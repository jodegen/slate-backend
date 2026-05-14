package de.jodegen.slate.training.command;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record AddExerciseSetCommand(
        @NotBlank String exerciseName,
        @Min(1) @Max(100) Integer reps,
        @DecimalMin("0.0") @DecimalMax("500.0") Double weightKg
) {}
