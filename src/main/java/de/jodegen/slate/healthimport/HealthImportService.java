package de.jodegen.slate.healthimport;

import de.jodegen.slate.common.DataSource;
import de.jodegen.slate.healthimport.dto.HealthImportRequest;
import de.jodegen.slate.healthimport.dto.HealthImportResponse;
import de.jodegen.slate.healthimport.dto.SleepSample;
import de.jodegen.slate.healthimport.dto.StepSample;
import de.jodegen.slate.healthimport.dto.WorkoutSample;
import de.jodegen.slate.routine.RoutineLog;
import de.jodegen.slate.routine.RoutineRepository;
import de.jodegen.slate.sleep.SleepLog;
import de.jodegen.slate.sleep.SleepRepository;
import de.jodegen.slate.training.TrainingDay;
import de.jodegen.slate.training.TrainingDayRepository;
import de.jodegen.slate.training.TrainingSession;
import de.jodegen.slate.training.TrainingSessionRepository;
import de.jodegen.slate.training.TrainingType;
import de.jodegen.slate.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class HealthImportService {

    private final SleepRepository sleepRepository;
    private final RoutineRepository routineRepository;
    private final TrainingDayRepository trainingDayRepository;
    private final TrainingSessionRepository trainingSessionRepository;
    private final WorkoutTypeMapper workoutTypeMapper;

    public HealthImportResponse processImport(User user, HealthImportRequest request) {
        int sleepUpserted = processSleep(user, request.sleep());
        int stepsProcessed = 0;
        int routineUpdated = 0;
        if (request.steps() != null) {
            int[] stepResults = processSteps(user, request.steps());
            stepsProcessed = stepResults[0];
            routineUpdated = stepResults[1];
        }
        int workoutsCreated = processWorkouts(user, request.workouts());
        return new HealthImportResponse(sleepUpserted, stepsProcessed, routineUpdated, workoutsCreated);
    }

    private int processSleep(User user, List<SleepSample> samples) {
        if (samples == null || samples.isEmpty()) return 0;

        Map<LocalDate, Long> secondsByDate = samples.stream()
                .filter(s -> s.sleepStage() == 3 || s.sleepStage() == 4 || s.sleepStage() == 5)
                .collect(Collectors.groupingBy(
                        s -> s.endDate().toInstant().atZone(ZoneOffset.UTC).toLocalDate(),
                        Collectors.summingLong(SleepSample::durationSeconds)
                ));

        int upserted = 0;
        for (Map.Entry<LocalDate, Long> entry : secondsByDate.entrySet()) {
            LocalDate date = entry.getKey();
            int durationMinutes = (int) Math.round(entry.getValue() / 60.0);

            SleepLog log = sleepRepository.findByUserAndDate(user, date)
                    .orElseGet(() -> SleepLog.builder().user(user).date(date).build());
            log.setDurationMinutes(durationMinutes);
            log.setSource(DataSource.HEALTH_IMPORT);
            sleepRepository.save(log);
            upserted++;
        }
        return upserted;
    }

    private int[] processSteps(User user, List<StepSample> samples) {
        int stepsProcessed = 0;
        int routineUpdated = 0;

        for (StepSample sample : samples) {
            stepsProcessed++;
            if (sample.value() >= 10000) {
                LocalDate date = sample.date().toInstant().atZone(ZoneOffset.UTC).toLocalDate();
                RoutineLog log = routineRepository.findByUserAndDate(user, date)
                        .orElseGet(() -> RoutineLog.builder().user(user).date(date).completedItems(new ArrayList<>()).build());
                if (!log.getCompletedItems().contains("steps")) {
                    log.getCompletedItems().add("steps");
                    routineRepository.save(log);
                    routineUpdated++;
                }
            }
        }
        return new int[]{stepsProcessed, routineUpdated};
    }

    private int processWorkouts(User user, List<WorkoutSample> samples) {
        if (samples == null || samples.isEmpty()) return 0;

        int created = 0;
        for (WorkoutSample sample : samples) {
            LocalDate date = sample.startDate().toInstant().atZone(ZoneOffset.UTC).toLocalDate();
            TrainingType type = workoutTypeMapper.map(sample.workoutType());
            int durationMinutes = (int) Math.round(sample.durationSeconds() / 60.0);

            TrainingDay day = trainingDayRepository.findByUserAndDate(user, date)
                    .orElseGet(() -> {
                        TrainingDay newDay = TrainingDay.builder()
                                .user(user)
                                .date(date)
                                .plannedType(plannedTypeForDate(date))
                                .sessions(new ArrayList<>())
                                .build();
                        return trainingDayRepository.save(newDay);
                    });

            List<TrainingSession> existing = trainingSessionRepository.findByTrainingDay(day);
            boolean duplicate = existing.stream().anyMatch(s ->
                    s.getSource() == DataSource.HEALTH_IMPORT
                    && sample.workoutType().equals(s.getSourceType())
                    && durationMinutes == s.getDurationMinutes());
            if (duplicate) continue;

            TrainingSession session = TrainingSession.builder()
                    .trainingDay(day)
                    .type(type)
                    .durationMinutes(durationMinutes)
                    .source(DataSource.HEALTH_IMPORT)
                    .sourceType(sample.workoutType())
                    .build();
            trainingSessionRepository.save(session);
            created++;
        }
        return created;
    }

    private TrainingType plannedTypeForDate(LocalDate date) {
        return switch (date.getDayOfWeek()) {
            case MONDAY, FRIDAY -> TrainingType.PUSH;
            case WEDNESDAY -> TrainingType.PULL;
            case TUESDAY, THURSDAY, SATURDAY -> TrainingType.CARDIO;
            case SUNDAY -> TrainingType.REST;
        };
    }
}
