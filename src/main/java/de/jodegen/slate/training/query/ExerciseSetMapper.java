package de.jodegen.slate.training.query;

import de.jodegen.slate.training.ExerciseSet;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ExerciseSetMapper {
    ExerciseSetView toView(ExerciseSet exerciseSet);
    List<ExerciseSetView> toViewList(List<ExerciseSet> exerciseSets);
}
