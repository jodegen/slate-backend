package de.jodegen.slate.training.command;

import de.jodegen.slate.common.DataSource;
import de.jodegen.slate.common.exception.ConflictException;
import de.jodegen.slate.common.exception.ResourceNotFoundException;
import de.jodegen.slate.common.exception.ValidationException;
import de.jodegen.slate.training.ExerciseSet;
import de.jodegen.slate.training.TrainingDay;
import de.jodegen.slate.training.TrainingDayRepository;
import de.jodegen.slate.training.TrainingSession;
import de.jodegen.slate.training.TrainingType;
import de.jodegen.slate.training.query.TrainingDayMapper;
import de.jodegen.slate.training.query.TrainingDayView;
import de.jodegen.slate.training.query.TrainingSessionView;
import de.jodegen.slate.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TrainingCommandServiceTest {

    @Mock TrainingDayRepository trainingDayRepository;
    @Mock TrainingDayMapper trainingDayMapper;
    @InjectMocks TrainingCommandService commandService;

    private User user;
    private LocalDate date;
    private TrainingDay day;
    private TrainingSession session;
    private TrainingDayView dayView;

    @BeforeEach
    void setUp() {
        user = User.builder().id(UUID.randomUUID()).email("test@test.com").name("Test").build();
        date = LocalDate.of(2026, 5, 14);

        session = TrainingSession.builder()
                .id(UUID.randomUUID())
                .type(TrainingType.PUSH)
                .source(DataSource.MANUAL)
                .createdAt(Instant.now())
                .sets(new ArrayList<>())
                .build();

        day = TrainingDay.builder()
                .id(UUID.randomUUID())
                .user(user)
                .date(date)
                .plannedType(TrainingType.PUSH)
                .createdAt(Instant.now())
                .sessions(new ArrayList<>(List.of(session)))
                .build();
        session.setTrainingDay(day);

        dayView = new TrainingDayView(day.getId(), date, TrainingType.PUSH,
                List.of(new TrainingSessionView(session.getId(), TrainingType.PUSH, null, DataSource.MANUAL, List.of(), Instant.now())),
                day.getCreatedAt());
    }

    @Test
    void createDay_success_createsDefaultSession() {
        when(trainingDayRepository.existsByUserAndDate(user, date)).thenReturn(false);
        when(trainingDayRepository.save(any())).thenReturn(day);
        when(trainingDayMapper.toView(day)).thenReturn(dayView);

        TrainingDayView result = commandService.createDay(user, new CreateTrainingDayCommand(date, TrainingType.PUSH));

        assertThat(result).isEqualTo(dayView);
        verify(trainingDayRepository).save(any(TrainingDay.class));
    }

    @Test
    void createDay_duplicateDate_throwsConflict() {
        when(trainingDayRepository.existsByUserAndDate(user, date)).thenReturn(true);

        assertThatThrownBy(() -> commandService.createDay(user, new CreateTrainingDayCommand(date, TrainingType.PUSH)))
                .isInstanceOf(ConflictException.class);

        verify(trainingDayRepository, never()).save(any());
    }

    @Test
    void addSession_success() {
        when(trainingDayRepository.findByUserAndDate(user, date)).thenReturn(Optional.of(day));
        when(trainingDayRepository.save(day)).thenReturn(day);
        when(trainingDayMapper.toView(day)).thenReturn(dayView);

        TrainingDayView result = commandService.addSession(user, date, new AddSessionCommand(TrainingType.PULL, 60, null));

        assertThat(result).isEqualTo(dayView);
        verify(trainingDayRepository).save(day);
    }

    @Test
    void addSession_dayNotFound_throwsResourceNotFoundException() {
        when(trainingDayRepository.findByUserAndDate(user, date)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> commandService.addSession(user, date, new AddSessionCommand(TrainingType.PULL, 60, null)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateSession_success() {
        when(trainingDayRepository.findByUserAndDate(user, date)).thenReturn(Optional.of(day));
        when(trainingDayRepository.save(day)).thenReturn(day);
        when(trainingDayMapper.toView(day)).thenReturn(dayView);

        TrainingDayView result = commandService.updateSession(user, date, session.getId(), new UpdateSessionCommand(TrainingType.CARDIO, 45));

        assertThat(result).isEqualTo(dayView);
        assertThat(session.getType()).isEqualTo(TrainingType.CARDIO);
        assertThat(session.getDurationMinutes()).isEqualTo(45);
    }

    @Test
    void updateSession_notFound_throwsResourceNotFoundException() {
        when(trainingDayRepository.findByUserAndDate(user, date)).thenReturn(Optional.of(day));

        assertThatThrownBy(() -> commandService.updateSession(user, date, UUID.randomUUID(), new UpdateSessionCommand(TrainingType.CARDIO, 45)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void deleteSession_success() {
        TrainingSession extraSession = TrainingSession.builder()
                .id(UUID.randomUUID()).type(TrainingType.PULL).source(DataSource.MANUAL)
                .sets(new ArrayList<>()).build();
        day.getSessions().add(extraSession);

        when(trainingDayRepository.findByUserAndDate(user, date)).thenReturn(Optional.of(day));
        when(trainingDayRepository.save(day)).thenReturn(day);
        when(trainingDayMapper.toView(day)).thenReturn(dayView);

        TrainingDayView result = commandService.deleteSession(user, date, session.getId());

        assertThat(result).isEqualTo(dayView);
        assertThat(day.getSessions()).doesNotContain(session);
    }

    @Test
    void deleteSession_lastSession_throwsValidationException() {
        when(trainingDayRepository.findByUserAndDate(user, date)).thenReturn(Optional.of(day));

        assertThatThrownBy(() -> commandService.deleteSession(user, date, session.getId()))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("last session");
    }

    @Test
    void deleteSession_notFound_throwsResourceNotFoundException() {
        when(trainingDayRepository.findByUserAndDate(user, date)).thenReturn(Optional.of(day));

        assertThatThrownBy(() -> commandService.deleteSession(user, date, UUID.randomUUID()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void addSet_success() {
        when(trainingDayRepository.findByUserAndDate(user, date)).thenReturn(Optional.of(day));
        when(trainingDayRepository.save(day)).thenReturn(day);
        when(trainingDayMapper.toView(day)).thenReturn(dayView);

        TrainingDayView result = commandService.addSet(user, date, session.getId(),
                new AddExerciseSetCommand("Bench Press", 10, 80.0));

        assertThat(result).isEqualTo(dayView);
        assertThat(session.getSets()).hasSize(1);
    }

    @Test
    void deleteDay_success() {
        when(trainingDayRepository.findByUserAndDate(user, date)).thenReturn(Optional.of(day));

        commandService.deleteDay(user, date);

        verify(trainingDayRepository).delete(day);
    }

    @Test
    void deleteDay_notFound_throwsResourceNotFoundException() {
        when(trainingDayRepository.findByUserAndDate(user, date)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> commandService.deleteDay(user, date))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(trainingDayRepository, never()).delete(any());
    }
}
