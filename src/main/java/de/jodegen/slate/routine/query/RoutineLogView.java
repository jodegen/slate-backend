package de.jodegen.slate.routine.query;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record RoutineLogView(UUID id, LocalDate date, List<String> completedItems, Instant createdAt) {}
