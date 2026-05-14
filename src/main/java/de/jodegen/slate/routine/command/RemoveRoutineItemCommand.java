package de.jodegen.slate.routine.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RemoveRoutineItemCommand(
        @NotNull @NotBlank String item
) {}
