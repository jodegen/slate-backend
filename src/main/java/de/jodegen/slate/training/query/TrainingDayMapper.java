package de.jodegen.slate.training.query;

import de.jodegen.slate.training.TrainingDay;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring", uses = TrainingSessionMapper.class)
public interface TrainingDayMapper {
    TrainingDayView toView(TrainingDay trainingDay);

    @Mapping(target = "sessionCount", expression = "java(trainingDay.getSessions().size())")
    TrainingDaySummaryView toSummaryView(TrainingDay trainingDay);

    List<TrainingDaySummaryView> toSummaryViewList(List<TrainingDay> trainingDays);
}
