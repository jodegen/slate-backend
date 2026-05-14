package de.jodegen.slate.weight.command;

import de.jodegen.slate.common.exception.ConflictException;
import de.jodegen.slate.common.exception.ResourceNotFoundException;
import de.jodegen.slate.user.User;
import de.jodegen.slate.weight.WeightEntry;
import de.jodegen.slate.weight.WeightRepository;
import de.jodegen.slate.weight.query.WeightEntryMapper;
import de.jodegen.slate.weight.query.WeightEntryView;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class WeightCommandService {

    private final WeightRepository weightRepository;
    private final WeightEntryMapper mapper;

    @Transactional
    public WeightEntryView create(User user, CreateWeightCommand command) {
        if (weightRepository.existsByUserAndDate(user, command.date())) {
            throw new ConflictException("Weight entry already exists for " + command.date());
        }
        WeightEntry entry = WeightEntry.builder()
                .user(user)
                .date(command.date())
                .kg(command.kg())
                .build();
        return mapper.toView(weightRepository.save(entry));
    }

    @Transactional
    public WeightEntryView upsert(User user, LocalDate date, UpdateWeightCommand command) {
        WeightEntry entry = weightRepository.findByUserAndDate(user, date)
                .map(existing -> {
                    existing.setKg(command.kg());
                    return existing;
                })
                .orElseGet(() -> WeightEntry.builder()
                        .user(user)
                        .date(date)
                        .kg(command.kg())
                        .build());
        return mapper.toView(weightRepository.save(entry));
    }

    @Transactional
    public void delete(User user, LocalDate date) {
        WeightEntry entry = weightRepository.findByUserAndDate(user, date)
                .orElseThrow(() -> new ResourceNotFoundException("No weight entry for " + date));
        weightRepository.delete(entry);
    }
}
