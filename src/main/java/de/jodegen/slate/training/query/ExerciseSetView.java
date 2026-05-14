package de.jodegen.slate.training.query;

import de.jodegen.slate.training.ExerciseSet;
import de.jodegen.slate.training.TrainingType;

import java.time.Instant;
import java.util.UUID;

public record ExerciseSetView(
        UUID id,
        String exerciseName,
        Integer reps,
        Double weightKg,
        Instant createdAt
) {}
