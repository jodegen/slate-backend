package de.jodegen.slate.weight.query;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record WeightEntryView(UUID id, LocalDate date, Double kg, Instant createdAt) {}
