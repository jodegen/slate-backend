package de.jodegen.slate.healthimport.dto;

import jakarta.validation.constraints.NotBlank;

public record SleepImportRequest(
        @NotBlank String sleepStartTimes,
        @NotBlank String sleepEndTimes,
        @NotBlank String sleepPhases
) {}
