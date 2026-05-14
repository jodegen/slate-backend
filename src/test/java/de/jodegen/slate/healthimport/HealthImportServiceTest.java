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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HealthImportServiceTest {

    @Mock SleepRepository sleepRepository;
    @Mock RoutineRepository routineRepository;
    @Mock TrainingDayRepository trainingDayRepository;
    @Mock TrainingSessionRepository trainingSessionRepository;
    @Mock WorkoutTypeMapper workoutTypeMapper;
    @InjectMocks HealthImportService service;

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder().id(UUID.randomUUID()).email("test@test.com").name("Test").build();
    }

    // --- Sleep tests ---

    @Test
    void sleep_stages3_4_5_summed_correctly() {
        // 3 sleep samples: core (3), deep (4), rem (5) — all on same night (endDate = 2026-05-14 UTC)
        OffsetDateTime endDate = OffsetDateTime.of(2026, 5, 14, 7, 0, 0, 0, ZoneOffset.UTC);
        List<SleepSample> samples = List.of(
                new SleepSample(3, endDate.minusHours(4), endDate.minusHours(2), 7200),
                new SleepSample(4, endDate.minusHours(2), endDate.minusHours(1), 3600),
                new SleepSample(5, endDate.minusHours(1), endDate, 3600)
        );
        HealthImportRequest request = new HealthImportRequest(null, samples, null);

        when(sleepRepository.findByUserAndDate(eq(user), any())).thenReturn(Optional.empty());
        when(sleepRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        HealthImportResponse result = service.processImport(user, request);

        assertThat(result.sleepUpserted()).isEqualTo(1);
        ArgumentCaptor<SleepLog> captor = ArgumentCaptor.forClass(SleepLog.class);
        verify(sleepRepository).save(captor.capture());
        // 7200 + 3600 + 3600 = 14400 seconds = 240 minutes
        assertThat(captor.getValue().getDurationMinutes()).isEqualTo(240);
        assertThat(captor.getValue().getSource()).isEqualTo(DataSource.HEALTH_IMPORT);
    }

    @Test
    void sleep_stages0_and_2_are_ignored() {
        OffsetDateTime endDate = OffsetDateTime.of(2026, 5, 14, 7, 0, 0, 0, ZoneOffset.UTC);
        List<SleepSample> samples = List.of(
                new SleepSample(0, endDate.minusHours(8), endDate, 28800), // inBed — ignored
                new SleepSample(2, endDate.minusHours(1), endDate, 3600)   // awake — ignored
        );
        HealthImportRequest request = new HealthImportRequest(null, samples, null);

        HealthImportResponse result = service.processImport(user, request);

        assertThat(result.sleepUpserted()).isEqualTo(0);
        verify(sleepRepository, never()).save(any());
    }

    @Test
    void sleep_upserts_existing_sleep_log() {
        OffsetDateTime endDate = OffsetDateTime.of(2026, 5, 14, 7, 0, 0, 0, ZoneOffset.UTC);
        LocalDate date = LocalDate.of(2026, 5, 14);
        SleepLog existing = SleepLog.builder()
                .id(UUID.randomUUID()).user(user).date(date).durationMinutes(300).source(DataSource.MANUAL)
                .build();
        List<SleepSample> samples = List.of(
                new SleepSample(3, endDate.minusHours(2), endDate, 7200)
        );
        HealthImportRequest request = new HealthImportRequest(null, samples, null);

        when(sleepRepository.findByUserAndDate(user, date)).thenReturn(Optional.of(existing));
        when(sleepRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        HealthImportResponse result = service.processImport(user, request);

        assertThat(result.sleepUpserted()).isEqualTo(1);
        ArgumentCaptor<SleepLog> captor = ArgumentCaptor.forClass(SleepLog.class);
        verify(sleepRepository).save(captor.capture());
        assertThat(captor.getValue().getId()).isEqualTo(existing.getId());
        assertThat(captor.getValue().getDurationMinutes()).isEqualTo(120);
        assertThat(captor.getValue().getSource()).isEqualTo(DataSource.HEALTH_IMPORT);
    }

    // --- Steps tests ---

    @Test
    void steps_value_gte_10000_adds_steps_to_routine() {
        LocalDate date = LocalDate.of(2026, 5, 14);
        OffsetDateTime dt = OffsetDateTime.of(2026, 5, 14, 12, 0, 0, 0, ZoneOffset.UTC);
        List<StepSample> steps = List.of(new StepSample(12000, dt));
        HealthImportRequest request = new HealthImportRequest(steps, null, null);

        when(routineRepository.findByUserAndDate(user, date)).thenReturn(Optional.empty());
        when(routineRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        HealthImportResponse result = service.processImport(user, request);

        assertThat(result.stepsProcessed()).isEqualTo(1);
        assertThat(result.routineUpdated()).isEqualTo(1);
        ArgumentCaptor<RoutineLog> captor = ArgumentCaptor.forClass(RoutineLog.class);
        verify(routineRepository).save(captor.capture());
        assertThat(captor.getValue().getCompletedItems()).contains("steps");
    }

    @Test
    void steps_value_lt_10000_does_not_update_routine() {
        OffsetDateTime dt = OffsetDateTime.of(2026, 5, 14, 12, 0, 0, 0, ZoneOffset.UTC);
        List<StepSample> steps = List.of(new StepSample(9999, dt));
        HealthImportRequest request = new HealthImportRequest(steps, null, null);

        HealthImportResponse result = service.processImport(user, request);

        assertThat(result.stepsProcessed()).isEqualTo(1);
        assertThat(result.routineUpdated()).isEqualTo(0);
        verify(routineRepository, never()).save(any());
    }

    @Test
    void steps_idempotent_when_steps_already_in_routine() {
        LocalDate date = LocalDate.of(2026, 5, 14);
        OffsetDateTime dt = OffsetDateTime.of(2026, 5, 14, 12, 0, 0, 0, ZoneOffset.UTC);
        List<StepSample> steps = List.of(new StepSample(15000, dt));
        RoutineLog existing = RoutineLog.builder()
                .id(UUID.randomUUID()).user(user).date(date)
                .completedItems(new ArrayList<>(List.of("steps", "water")))
                .build();
        HealthImportRequest request = new HealthImportRequest(steps, null, null);

        when(routineRepository.findByUserAndDate(user, date)).thenReturn(Optional.of(existing));

        HealthImportResponse result = service.processImport(user, request);

        assertThat(result.stepsProcessed()).isEqualTo(1);
        assertThat(result.routineUpdated()).isEqualTo(0);
        verify(routineRepository, never()).save(any());
    }

    // --- Workout tests ---

    @Test
    void workout_creates_new_training_day_with_session() {
        LocalDate date = LocalDate.of(2026, 5, 13); // Wednesday → PULL
        OffsetDateTime start = OffsetDateTime.of(2026, 5, 13, 10, 0, 0, 0, ZoneOffset.UTC);
        OffsetDateTime end = start.plusMinutes(47);
        List<WorkoutSample> workouts = List.of(new WorkoutSample("Indoor Cycling", 2820, start, end));
        HealthImportRequest request = new HealthImportRequest(null, null, workouts);

        TrainingDay savedDay = TrainingDay.builder()
                .id(UUID.randomUUID()).user(user).date(date).plannedType(TrainingType.PULL).sessions(new ArrayList<>()).build();

        when(workoutTypeMapper.map("Indoor Cycling")).thenReturn(TrainingType.CARDIO);
        when(trainingDayRepository.findByUserAndDate(user, date)).thenReturn(Optional.empty());
        when(trainingDayRepository.save(any(TrainingDay.class))).thenReturn(savedDay);
        when(trainingSessionRepository.findByTrainingDay(savedDay)).thenReturn(List.of());
        when(trainingSessionRepository.save(any(TrainingSession.class))).thenAnswer(inv -> inv.getArgument(0));

        HealthImportResponse result = service.processImport(user, request);

        assertThat(result.workoutsCreated()).isEqualTo(1);
        ArgumentCaptor<TrainingDay> dayCaptor = ArgumentCaptor.forClass(TrainingDay.class);
        verify(trainingDayRepository).save(dayCaptor.capture());
        assertThat(dayCaptor.getValue().getPlannedType()).isEqualTo(TrainingType.PULL);

        ArgumentCaptor<TrainingSession> sessionCaptor = ArgumentCaptor.forClass(TrainingSession.class);
        verify(trainingSessionRepository).save(sessionCaptor.capture());
        TrainingSession session = sessionCaptor.getValue();
        assertThat(session.getType()).isEqualTo(TrainingType.CARDIO);
        assertThat(session.getDurationMinutes()).isEqualTo(47);
        assertThat(session.getSource()).isEqualTo(DataSource.HEALTH_IMPORT);
        assertThat(session.getSourceType()).isEqualTo("Indoor Cycling");
    }

    @Test
    void workout_adds_session_to_existing_training_day() {
        LocalDate date = LocalDate.of(2026, 5, 14);
        OffsetDateTime start = OffsetDateTime.of(2026, 5, 14, 10, 0, 0, 0, ZoneOffset.UTC);
        List<WorkoutSample> workouts = List.of(new WorkoutSample("Running", 3600, start, start.plusHours(1)));
        HealthImportRequest request = new HealthImportRequest(null, null, workouts);

        TrainingDay existingDay = TrainingDay.builder()
                .id(UUID.randomUUID()).user(user).date(date).plannedType(TrainingType.PULL).sessions(new ArrayList<>()).build();

        when(workoutTypeMapper.map("Running")).thenReturn(TrainingType.CARDIO);
        when(trainingDayRepository.findByUserAndDate(user, date)).thenReturn(Optional.of(existingDay));
        when(trainingSessionRepository.findByTrainingDay(existingDay)).thenReturn(List.of());
        when(trainingSessionRepository.save(any(TrainingSession.class))).thenAnswer(inv -> inv.getArgument(0));

        HealthImportResponse result = service.processImport(user, request);

        assertThat(result.workoutsCreated()).isEqualTo(1);
        verify(trainingDayRepository, never()).save(any());
        verify(trainingSessionRepository).save(any(TrainingSession.class));
    }

    @Test
    void workout_deduplication_skips_duplicate() {
        LocalDate date = LocalDate.of(2026, 5, 14);
        OffsetDateTime start = OffsetDateTime.of(2026, 5, 14, 10, 0, 0, 0, ZoneOffset.UTC);
        List<WorkoutSample> workouts = List.of(new WorkoutSample("Indoor Cycling", 2820, start, start.plusMinutes(47)));
        HealthImportRequest request = new HealthImportRequest(null, null, workouts);

        TrainingSession duplicate = TrainingSession.builder()
                .id(UUID.randomUUID())
                .type(TrainingType.CARDIO)
                .source(DataSource.HEALTH_IMPORT)
                .sourceType("Indoor Cycling")
                .durationMinutes(47)
                .build();
        TrainingDay existingDay = TrainingDay.builder()
                .id(UUID.randomUUID()).user(user).date(date).plannedType(TrainingType.CARDIO).sessions(new ArrayList<>()).build();

        when(workoutTypeMapper.map("Indoor Cycling")).thenReturn(TrainingType.CARDIO);
        when(trainingDayRepository.findByUserAndDate(user, date)).thenReturn(Optional.of(existingDay));
        when(trainingSessionRepository.findByTrainingDay(existingDay)).thenReturn(List.of(duplicate));

        HealthImportResponse result = service.processImport(user, request);

        assertThat(result.workoutsCreated()).isEqualTo(0);
        verify(trainingSessionRepository, never()).save(any());
    }

    // --- Empty request ---

    @Test
    void empty_request_returns_all_zeros() {
        HealthImportRequest request = new HealthImportRequest(List.of(), List.of(), List.of());

        HealthImportResponse result = service.processImport(user, request);

        assertThat(result.sleepUpserted()).isEqualTo(0);
        assertThat(result.stepsProcessed()).isEqualTo(0);
        assertThat(result.routineUpdated()).isEqualTo(0);
        assertThat(result.workoutsCreated()).isEqualTo(0);
        verifyNoInteractions(sleepRepository, routineRepository, trainingDayRepository, trainingSessionRepository);
    }

    @Test
    void null_lists_returns_all_zeros() {
        HealthImportRequest request = new HealthImportRequest(null, null, null);

        HealthImportResponse result = service.processImport(user, request);

        assertThat(result.sleepUpserted()).isEqualTo(0);
        assertThat(result.stepsProcessed()).isEqualTo(0);
        assertThat(result.routineUpdated()).isEqualTo(0);
        assertThat(result.workoutsCreated()).isEqualTo(0);
        verifyNoInteractions(sleepRepository, routineRepository, trainingDayRepository, trainingSessionRepository);
    }
}
