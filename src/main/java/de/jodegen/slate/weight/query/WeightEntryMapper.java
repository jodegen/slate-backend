package de.jodegen.slate.weight.query;

import de.jodegen.slate.weight.WeightEntry;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface WeightEntryMapper {
    WeightEntryView toView(WeightEntry entry);
}
