package de.jodegen.slate.sleep.command;

import de.jodegen.slate.common.DataSource;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record CreateSleepCommand(
        @NotNull LocalDate date,
        @NotNull @Min(0) @Max(1440) Integer durationMinutes,
        DataSource source
) {}
