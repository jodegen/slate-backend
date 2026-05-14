package de.jodegen.slate.training.query;

import de.jodegen.slate.training.TrainingType;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record TrainingDayView(
        UUID id,
        LocalDate date,
        TrainingType plannedType,
        List<TrainingSessionView> sessions,
        Instant createdAt
) {}
