package de.jodegen.slate.sleep.command;

import de.jodegen.slate.common.DataSource;
import de.jodegen.slate.common.exception.ConflictException;
import de.jodegen.slate.common.exception.ResourceNotFoundException;
import de.jodegen.slate.sleep.SleepLog;
import de.jodegen.slate.sleep.SleepRepository;
import de.jodegen.slate.sleep.query.SleepLogMapper;
import de.jodegen.slate.sleep.query.SleepLogView;
import de.jodegen.slate.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SleepCommandServiceTest {

    @Mock SleepRepository sleepRepository;
    @Mock SleepLogMapper mapper;
    @InjectMocks SleepCommandService commandService;

    private User user;
    private LocalDate date;
    private SleepLog sleepLog;
    private SleepLogView view;

    @BeforeEach
    void setUp() {
        user = User.builder().id(UUID.randomUUID()).email("test@test.com").name("Test").build();
        date = LocalDate.of(2026, 5, 14);
        sleepLog = SleepLog.builder()
                .id(UUID.randomUUID()).user(user).date(date)
                .durationMinutes(480).source(DataSource.MANUAL).createdAt(Instant.now())
                .build();
        view = new SleepLogView(sleepLog.getId(), date, 480, DataSource.MANUAL, sleepLog.getCreatedAt());
    }

    @Test
    void shouldCreateEntry_whenValidCommand() {
        when(sleepRepository.existsByUserAndDate(user, date)).thenReturn(false);
        when(sleepRepository.save(any())).thenReturn(sleepLog);
        when(mapper.toView(sleepLog)).thenReturn(view);

        SleepLogView result = commandService.create(user, new CreateSleepCommand(date, 480, null));

        assertThat(result).isEqualTo(view);
        verify(sleepRepository).save(any(SleepLog.class));
    }

    @Test
    void shouldThrowConflict_whenEntryAlreadyExistsForDate() {
        when(sleepRepository.existsByUserAndDate(user, date)).thenReturn(true);

        assertThatThrownBy(() -> commandService.create(user, new CreateSleepCommand(date, 480, null)))
                .isInstanceOf(ConflictException.class);

        verify(sleepRepository, never()).save(any());
    }

    @Test
    void shouldUpdateExistingEntry_whenUpsertAndEntryExists() {
        when(sleepRepository.findByUserAndDate(user, date)).thenReturn(Optional.of(sleepLog));
        when(sleepRepository.save(sleepLog)).thenReturn(sleepLog);
        when(mapper.toView(sleepLog)).thenReturn(new SleepLogView(sleepLog.getId(), date, 500, DataSource.MANUAL, sleepLog.getCreatedAt()));

        SleepLogView result = commandService.upsert(user, date, new UpdateSleepCommand(500));

        assertThat(result.durationMinutes()).isEqualTo(500);
        assertThat(sleepLog.getDurationMinutes()).isEqualTo(500);
    }

    @Test
    void shouldCreateNewEntry_whenUpsertAndNoExistingEntry() {
        when(sleepRepository.findByUserAndDate(user, date)).thenReturn(Optional.empty());
        when(sleepRepository.save(any())).thenReturn(sleepLog);
        when(mapper.toView(sleepLog)).thenReturn(view);

        SleepLogView result = commandService.upsert(user, date, new UpdateSleepCommand(480));

        assertThat(result).isEqualTo(view);
        verify(sleepRepository).save(any(SleepLog.class));
    }

    @Test
    void shouldDelete_whenEntryExists() {
        when(sleepRepository.findByUserAndDate(user, date)).thenReturn(Optional.of(sleepLog));

        commandService.delete(user, date);

        verify(sleepRepository).delete(sleepLog);
    }

    @Test
    void shouldThrowNotFound_whenDeletingNonExistentEntry() {
        when(sleepRepository.findByUserAndDate(user, date)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> commandService.delete(user, date))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(sleepRepository, never()).delete(any());
    }
}
