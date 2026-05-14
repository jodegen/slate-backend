package de.jodegen.slate.weight.command;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

public record UpdateWeightCommand(
        @NotNull @DecimalMin("0.1") Double kg
) {}
