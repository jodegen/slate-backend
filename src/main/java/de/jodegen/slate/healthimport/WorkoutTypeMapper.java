package de.jodegen.slate.healthimport;

import de.jodegen.slate.training.TrainingType;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class WorkoutTypeMapper {

    private static final Map<String, TrainingType> MAPPINGS = Map.of(
            "Running",                          TrainingType.CARDIO,
            "Cycling",                          TrainingType.CARDIO,
            "Indoor Cycling",                   TrainingType.CARDIO,
            "High Intensity Interval Training", TrainingType.CARDIO,
            "Functional Strength Training",     TrainingType.PUSH,
            "Traditional Strength Training",    TrainingType.PUSH,
            "Core Training",                    TrainingType.PUSH,
            "Walking",                          TrainingType.CARDIO
    );

    public TrainingType map(String appleWorkoutType) {
        return MAPPINGS.getOrDefault(appleWorkoutType, TrainingType.CARDIO);
    }
}
