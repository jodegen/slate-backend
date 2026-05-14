package de.jodegen.slate.routine.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AddRoutineItemCommand(
        @NotNull @NotBlank String item
) {}
