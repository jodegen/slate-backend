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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HealthImportServiceTest {

    @Mock SleepRepository sleepRepository;
    @Mock RoutineRepository routineRepository;
    @InjectMocks HealthImportService service;

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder().id(UUID.randomUUID()).email("test@test.com").name("Test").build();
    }

    // --- Steps tests ---

    @Test
    void steps_totalGte10000_addsStepsToRoutine() {
        StepsImportRequest request = new StepsImportRequest(
                "3000\n3000\n4000",
                "2026-05-14T08:00:00+02:00\n2026-05-14T10:00:00+02:00\n2026-05-14T12:00:00+02:00"
        );

        when(routineRepository.findByUserAndDate(eq(user), any())).thenReturn(Optional.empty());
        when(routineRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        StepsImportResponse result = service.processSteps(user, request);

        assertThat(result.totalSteps()).isEqualTo(10000);
        assertThat(result.routineUpdated()).isTrue();
        ArgumentCaptor<RoutineLog> captor = ArgumentCaptor.forClass(RoutineLog.class);
        verify(routineRepository).save(captor.capture());
        assertThat(captor.getValue().getCompletedItems()).contains("steps");
    }

    @Test
    void steps_totalLt10000_doesNotUpdateRoutine() {
        StepsImportRequest request = new StepsImportRequest(
                "1000\n2000\n3000",
                "2026-05-14T08:00:00+02:00\n2026-05-14T10:00:00+02:00\n2026-05-14T12:00:00+02:00"
        );

        StepsImportResponse result = service.processSteps(user, request);

        assertThat(result.totalSteps()).isEqualTo(6000);
        assertThat(result.routineUpdated()).isFalse();
        verify(routineRepository, never()).save(any());
    }

    @Test
    void steps_idempotent_whenStepsAlreadyInRoutine() {
        LocalDate date = LocalDate.of(2026, 5, 14);
        RoutineLog existing = RoutineLog.builder()
                .id(UUID.randomUUID()).user(user).date(date)
                .completedItems(new ArrayList<>(List.of("steps", "water")))
                .build();
        StepsImportRequest request = new StepsImportRequest(
                "15000",
                "2026-05-14T08:00:00+02:00"
        );

        when(routineRepository.findByUserAndDate(user, date)).thenReturn(Optional.of(existing));

        StepsImportResponse result = service.processSteps(user, request);

        assertThat(result.routineUpdated()).isFalse();
        verify(routineRepository, never()).save(any());
    }

    @Test
    void steps_usesDateOfFirstEntry() {
        StepsImportRequest request = new StepsImportRequest(
                "12000\n0",
                "2026-05-14T23:00:00+02:00\n2026-05-15T00:00:00+02:00"
        );

        when(routineRepository.findByUserAndDate(eq(user), any())).thenReturn(Optional.empty());
        when(routineRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.processSteps(user, request);

        ArgumentCaptor<RoutineLog> captor = ArgumentCaptor.forClass(RoutineLog.class);
        verify(routineRepository).save(captor.capture());
        assertThat(captor.getValue().getDate()).isEqualTo(LocalDate.of(2026, 5, 14));
    }

    @Test
    void steps_mismatchedLengths_throwsValidationException() {
        StepsImportRequest request = new StepsImportRequest(
                "1000\n2000",
                "2026-05-14T08:00:00+02:00"
        );

        assertThatThrownBy(() -> service.processSteps(user, request))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void steps_emptyPayload_throwsValidationException() {
        StepsImportRequest request = new StepsImportRequest("", "");

        assertThatThrownBy(() -> service.processSteps(user, request))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void steps_invalidStepValue_throwsValidationException() {
        StepsImportRequest request = new StepsImportRequest(
                "abc",
                "2026-05-14T08:00:00+02:00"
        );

        assertThatThrownBy(() -> service.processSteps(user, request))
                .isInstanceOf(ValidationException.class);
    }

    // --- Sleep tests ---

    @Test
    void sleep_sumsNonAwakePhases() {
        // Core: 13 min, Awake: 1 min (excluded), Deep: 15 min → total = 28 min = 1680 sec
        SleepImportRequest request = new SleepImportRequest(
                "2026-04-19T02:00:00+02:00\n2026-04-19T02:13:00+02:00\n2026-04-19T02:14:00+02:00",
                "2026-04-19T02:13:00+02:00\n2026-04-19T02:14:00+02:00\n2026-04-19T02:29:00+02:00",
                "Core\nAwake\nDeep"
        );

        when(sleepRepository.findByUserAndDate(eq(user), any())).thenReturn(Optional.empty());
        when(sleepRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        SleepImportResponse result = service.processSleep(user, request);

        assertThat(result.durationMinutes()).isEqualTo(28);
        assertThat(result.upserted()).isTrue();
    }

    @Test
    void sleep_awakePhaseNotCounted() {
        SleepImportRequest request = new SleepImportRequest(
                "2026-04-19T02:00:00+02:00",
                "2026-04-19T02:30:00+02:00",
                "Awake"
        );

        when(sleepRepository.findByUserAndDate(eq(user), any())).thenReturn(Optional.empty());
        when(sleepRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        SleepImportResponse result = service.processSleep(user, request);

        assertThat(result.durationMinutes()).isEqualTo(0);
    }

    @Test
    void sleep_upsertsExistingSleepLog() {
        LocalDate date = LocalDate.of(2026, 4, 20);
        SleepLog existing = SleepLog.builder()
                .id(UUID.randomUUID()).user(user).date(date).durationMinutes(300).source(DataSource.MANUAL)
                .build();
        SleepImportRequest request = new SleepImportRequest(
                "2026-04-19T23:00:00+02:00",
                "2026-04-20T00:00:00+02:00",
                "Core"
        );

        when(sleepRepository.findByUserAndDate(user, date)).thenReturn(Optional.of(existing));
        when(sleepRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        SleepImportResponse result = service.processSleep(user, request);

        ArgumentCaptor<SleepLog> captor = ArgumentCaptor.forClass(SleepLog.class);
        verify(sleepRepository).save(captor.capture());
        assertThat(captor.getValue().getId()).isEqualTo(existing.getId());
        assertThat(captor.getValue().getDurationMinutes()).isEqualTo(60);
        assertThat(captor.getValue().getSource()).isEqualTo(DataSource.HEALTH_IMPORT);
    }

    @Test
    void sleep_dateIsLastEndTime() {
        // last endTime is on April 20th → date should be April 20th
        SleepImportRequest request = new SleepImportRequest(
                "2026-04-19T23:00:00+02:00\n2026-04-19T23:30:00+02:00",
                "2026-04-19T23:30:00+02:00\n2026-04-20T00:00:00+02:00",
                "Core\nREM"
        );

        when(sleepRepository.findByUserAndDate(eq(user), eq(LocalDate.of(2026, 4, 20)))).thenReturn(Optional.empty());
        when(sleepRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        SleepImportResponse result = service.processSleep(user, request);

        assertThat(result.date()).isEqualTo(LocalDate.of(2026, 4, 20));
    }

    @Test
    void sleep_allCountedPhases_areRecognized() {
        SleepImportRequest request = new SleepImportRequest(
                "2026-04-19T00:00:00+02:00\n2026-04-19T00:10:00+02:00\n2026-04-19T00:20:00+02:00\n2026-04-19T00:30:00+02:00",
                "2026-04-19T00:10:00+02:00\n2026-04-19T00:20:00+02:00\n2026-04-19T00:30:00+02:00\n2026-04-19T00:40:00+02:00",
                "Core\nDeep\nREM\nAsleep"
        );

        when(sleepRepository.findByUserAndDate(eq(user), any())).thenReturn(Optional.empty());
        when(sleepRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        SleepImportResponse result = service.processSleep(user, request);

        assertThat(result.durationMinutes()).isEqualTo(40);
    }

    @Test
    void sleep_unknownPhase_throwsValidationException() {
        SleepImportRequest request = new SleepImportRequest(
                "2026-04-19T02:00:00+02:00",
                "2026-04-19T02:30:00+02:00",
                "Dreaming"
        );

        assertThatThrownBy(() -> service.processSleep(user, request))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void sleep_mismatchedLengths_throwsValidationException() {
        SleepImportRequest request = new SleepImportRequest(
                "2026-04-19T02:00:00+02:00\n2026-04-19T03:00:00+02:00",
                "2026-04-19T02:30:00+02:00",
                "Core\nDeep"
        );

        assertThatThrownBy(() -> service.processSleep(user, request))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void sleep_crlfLineEndings_parsedCorrectly() {
        SleepImportRequest request = new SleepImportRequest(
                "2026-04-19T02:00:00+02:00\r\n2026-04-19T02:30:00+02:00",
                "2026-04-19T02:30:00+02:00\r\n2026-04-19T03:00:00+02:00",
                "Core\r\nREM"
        );

        when(sleepRepository.findByUserAndDate(eq(user), any())).thenReturn(Optional.empty());
        when(sleepRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        SleepImportResponse result = service.processSleep(user, request);

        assertThat(result.durationMinutes()).isEqualTo(60);
    }
}
