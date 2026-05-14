package de.jodegen.slate.sleep.query;

import de.jodegen.slate.sleep.SleepLog;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface SleepLogMapper {
    SleepLogView toView(SleepLog sleepLog);
}
