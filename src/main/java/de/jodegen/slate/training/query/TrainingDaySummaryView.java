package de.jodegen.slate.training.query;

import de.jodegen.slate.training.TrainingType;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record TrainingDaySummaryView(
        UUID id,
        LocalDate date,
        TrainingType plannedType,
        int sessionCount,
        Instant createdAt
) {}
