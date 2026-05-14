package de.jodegen.slate.routine.query;

import de.jodegen.slate.routine.RoutineLog;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface RoutineLogMapper {
    RoutineLogView toView(RoutineLog routineLog);
}
