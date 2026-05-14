package de.jodegen.slate.healthimport.dto;

import jakarta.validation.constraints.NotBlank;

public record StepsImportRequest(
        @NotBlank String steps,
        @NotBlank String dateTimes
) {}
