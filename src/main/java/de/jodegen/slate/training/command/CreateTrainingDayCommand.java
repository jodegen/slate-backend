package de.jodegen.slate.training.command;

import de.jodegen.slate.training.TrainingType;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record CreateTrainingDayCommand(
        @NotNull LocalDate date,
        @NotNull TrainingType plannedType
) {}
