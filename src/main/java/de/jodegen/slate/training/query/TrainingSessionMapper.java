package de.jodegen.slate.training.query;

import de.jodegen.slate.training.TrainingSession;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring", uses = ExerciseSetMapper.class)
public interface TrainingSessionMapper {
    TrainingSessionView toView(TrainingSession trainingSession);
    List<TrainingSessionView> toViewList(List<TrainingSession> trainingSessions);
}
