package de.jodegen.slate.weight.command;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record CreateWeightCommand(
        @NotNull LocalDate date,
        @NotNull @DecimalMin("0.1") Double kg
) {}
