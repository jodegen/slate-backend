package de.jodegen.slate.healthimport.dto;

public record HealthImportResponse(
        int sleepUpserted,
        int stepsProcessed,
        int routineUpdated,
        int workoutsCreated
) {}
