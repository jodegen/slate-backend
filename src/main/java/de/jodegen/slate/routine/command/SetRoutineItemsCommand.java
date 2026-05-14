package de.jodegen.slate.routine.command;

import jakarta.validation.constraints.NotNull;

import java.util.List;

public record SetRoutineItemsCommand(
        @NotNull List<String> items
) {}
