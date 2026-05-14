package de.jodegen.slate.training.command;

import de.jodegen.slate.training.TrainingType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record UpdateSessionCommand(
        @NotNull TrainingType type,
        @Min(1) @Max(480) Integer durationMinutes
) {}
