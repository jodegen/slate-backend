package de.jodegen.slate.healthimport;

import de.jodegen.slate.common.DataSource;
import de.jodegen.slate.common.exception.ValidationException;
import de.jodegen.slate.healthimport.dto.SleepImportRequest;
import de.jodegen.slate.healthimport.dto.SleepImportResponse;
import de.jodegen.slate.healthimport.dto.StepsImportRequest;
import de.jodegen.slate.healthimport.dto.StepsImportResponse;
import de.jodegen.slate.routine.RoutineLog;
import de.jodegen.slate.routine.RoutineRepository;
import de.jodegen.slate.sleep.SleepLog;
import de.jodegen.slate.sleep.SleepRepository;
import de.jodegen.slate.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class HealthImportService {

    private static final Set<String> COUNTED_SLEEP_PHASES = Set.of("Core", "Deep", "REM", "Asleep");

    private final SleepRepository sleepRepository;
    private final RoutineRepository routineRepository;

    @Transactional
    public StepsImportResponse processSteps(User user, StepsImportRequest request) {
        String[] stepTokens = parseTokens(request.steps());
        String[] dateTokens = parseTokens(request.dateTimes());

        if (stepTokens.length == 0) {
            throw new ValidationException("steps payload must not be empty");
        }
        if (stepTokens.length != dateTokens.length) {
            throw new ValidationException("steps and dateTimes must have equal length");
        }

        int totalSteps = 0;
        for (String s : stepTokens) {
            try {
                totalSteps += Integer.parseInt(s);
            } catch (NumberFormatException e) {
                throw new ValidationException("Invalid step value: " + s);
            }
        }

        LocalDate date;
        try {
            date = OffsetDateTime.parse(dateTokens[0]).toLocalDate();
        } catch (DateTimeParseException e) {
            throw new ValidationException("Invalid dateTime format: " + dateTokens[0]);
        }

        boolean updated = false;
        if (totalSteps >= 10000) {
            RoutineLog log = routineRepository.findByUserAndDate(user, date)
                    .orElseGet(() -> RoutineLog.builder().user(user).date(date).completedItems(new ArrayList<>()).build());
            if (!log.getCompletedItems().contains("steps")) {
                log.getCompletedItems().add("steps");
                routineRepository.save(log);
                updated = true;
            }
        }

        return new StepsImportResponse(totalSteps, updated);
    }

    @Transactional
    public SleepImportResponse processSleep(User user, SleepImportRequest request) {
        String[] starts = parseTokens(request.sleepStartTimes());
        String[] ends = parseTokens(request.sleepEndTimes());
        String[] phases = parseTokens(request.sleepPhases());

        if (starts.length == 0) {
            throw new ValidationException("sleepStartTimes must not be empty");
        }
        if (starts.length != ends.length || starts.length != phases.length) {
            throw new ValidationException("sleepStartTimes, sleepEndTimes, and sleepPhases must have equal length");
        }

        long totalSeconds = 0;
        for (int i = 0; i < starts.length; i++) {
            if (!COUNTED_SLEEP_PHASES.contains(phases[i]) && !"Awake".equals(phases[i])) {
                throw new ValidationException("Unknown sleep phase: " + phases[i]);
            }
            if (COUNTED_SLEEP_PHASES.contains(phases[i])) {
                try {
                    OffsetDateTime start = OffsetDateTime.parse(starts[i]);
                    OffsetDateTime end = OffsetDateTime.parse(ends[i]);
                    totalSeconds += ChronoUnit.SECONDS.between(start, end);
                } catch (DateTimeParseException e) {
                    throw new ValidationException("Invalid timestamp in sleep data at index " + i);
                }
            }
        }

        int durationMinutes = (int) (totalSeconds / 60);

        LocalDate date;
        try {
            date = OffsetDateTime.parse(ends[ends.length - 1]).toLocalDate();
        } catch (DateTimeParseException e) {
            throw new ValidationException("Invalid timestamp format in sleepEndTimes");
        }

        SleepLog log = sleepRepository.findByUserAndDate(user, date)
                .orElseGet(() -> SleepLog.builder().user(user).date(date).build());
        log.setDurationMinutes(durationMinutes);
        log.setSource(DataSource.HEALTH_IMPORT);
        sleepRepository.save(log);

        return new SleepImportResponse(date, durationMinutes, true);
    }

    private String[] parseTokens(String raw) {
        return Arrays.stream(raw.split("\r?\n"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toArray(String[]::new);
    }
}
