package de.jodegen.slate.sleep.command;

import de.jodegen.slate.common.DataSource;
import de.jodegen.slate.common.exception.ConflictException;
import de.jodegen.slate.common.exception.ResourceNotFoundException;
import de.jodegen.slate.sleep.SleepLog;
import de.jodegen.slate.sleep.SleepRepository;
import de.jodegen.slate.sleep.query.SleepLogMapper;
import de.jodegen.slate.sleep.query.SleepLogView;
import de.jodegen.slate.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class SleepCommandService {

    private final SleepRepository sleepRepository;
    private final SleepLogMapper mapper;

    @Transactional
    public SleepLogView create(User user, CreateSleepCommand command) {
        if (sleepRepository.existsByUserAndDate(user, command.date())) {
            throw new ConflictException("Sleep log already exists for " + command.date());
        }
        SleepLog sleepLog = SleepLog.builder()
                .user(user)
                .date(command.date())
                .durationMinutes(command.durationMinutes())
                .source(command.source() != null ? command.source() : DataSource.MANUAL)
                .build();
        return mapper.toView(sleepRepository.save(sleepLog));
    }

    @Transactional
    public SleepLogView upsert(User user, LocalDate date, UpdateSleepCommand command) {
        SleepLog sleepLog = sleepRepository.findByUserAndDate(user, date)
                .map(existing -> {
                    existing.setDurationMinutes(command.durationMinutes());
                    return existing;
                })
                .orElseGet(() -> SleepLog.builder()
                        .user(user)
                        .date(date)
                        .durationMinutes(command.durationMinutes())
                        .build());
        return mapper.toView(sleepRepository.save(sleepLog));
    }

    @Transactional
    public void delete(User user, LocalDate date) {
        SleepLog sleepLog = sleepRepository.findByUserAndDate(user, date)
                .orElseThrow(() -> new ResourceNotFoundException("No sleep log for " + date));
        sleepRepository.delete(sleepLog);
    }
}
