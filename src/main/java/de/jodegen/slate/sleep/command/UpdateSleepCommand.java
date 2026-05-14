package de.jodegen.slate.sleep.command;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record UpdateSleepCommand(
        @NotNull @Min(0) @Max(1440) Integer durationMinutes
) {}
