package de.jodegen.slate.training.query;

import de.jodegen.slate.common.DataSource;
import de.jodegen.slate.training.TrainingType;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record TrainingSessionView(
        UUID id,
        TrainingType type,
        Integer durationMinutes,
        DataSource source,
        List<ExerciseSetView> sets,
        Instant createdAt
) {}
