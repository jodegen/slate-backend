package de.jodegen.slate.sleep.query;

import de.jodegen.slate.common.DataSource;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record SleepLogView(UUID id, LocalDate date, int durationMinutes, DataSource source, Instant createdAt) {}
