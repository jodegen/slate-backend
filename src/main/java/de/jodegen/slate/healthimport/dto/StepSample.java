package de.jodegen.slate.healthimport.dto;

import jakarta.validation.constraints.NotNull;

import java.time.OffsetDateTime;

public record StepSample(
        @NotNull int value,
        @NotNull OffsetDateTime date
) {}
