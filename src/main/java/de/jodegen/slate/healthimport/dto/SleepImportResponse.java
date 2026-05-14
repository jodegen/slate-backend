package de.jodegen.slate.healthimport.dto;

import java.time.LocalDate;

public record SleepImportResponse(LocalDate date, int durationMinutes, boolean upserted) {}
