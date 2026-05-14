package de.jodegen.slate.routine.command;

import de.jodegen.slate.common.exception.ResourceNotFoundException;
import de.jodegen.slate.routine.RoutineLog;
import de.jodegen.slate.routine.RoutineRepository;
import de.jodegen.slate.routine.query.RoutineLogMapper;
import de.jodegen.slate.routine.query.RoutineLogView;
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
class RoutineCommandServiceTest {

    @Mock RoutineRepository routineRepository;
    @Mock RoutineLogMapper mapper;
    @InjectMocks RoutineCommandService commandService;

    private User user;
    private LocalDate date;
    private RoutineLog routineLog;
    private RoutineLogView view;

    @BeforeEach
    void setUp() {
        user = User.builder().id(UUID.randomUUID()).email("test@test.com").name("Test").build();
        date = LocalDate.of(2026, 5, 14);
        routineLog = RoutineLog.builder()
                .id(UUID.randomUUID()).user(user).date(date)
                .completedItems(new ArrayList<>(List.of("exercise")))
                .createdAt(Instant.now())
                .build();
        view = new RoutineLogView(routineLog.getId(), date, routineLog.getCompletedItems(), routineLog.getCreatedAt());
    }

    @Test
    void shouldCreateNewLog_whenAddingItemAndNoLogExists() {
        when(routineRepository.findByUserAndDate(user, date)).thenReturn(Optional.empty());
        when(routineRepository.save(any())).thenReturn(routineLog);
        when(mapper.toView(routineLog)).thenReturn(view);

        RoutineLogView result = commandService.addItem(user, date, new AddRoutineItemCommand("exercise"));

        assertThat(result).isEqualTo(view);
        verify(routineRepository).save(any(RoutineLog.class));
    }

    @Test
    void shouldAddItem_whenLogExists() {
        RoutineLog logWithoutItem = RoutineLog.builder()
                .id(UUID.randomUUID()).user(user).date(date)
                .completedItems(new ArrayList<>())
                .createdAt(Instant.now())
                .build();
        when(routineRepository.findByUserAndDate(user, date)).thenReturn(Optional.of(logWithoutItem));
        when(routineRepository.save(logWithoutItem)).thenReturn(logWithoutItem);
        when(mapper.toView(logWithoutItem)).thenReturn(view);

        commandService.addItem(user, date, new AddRoutineItemCommand("exercise"));

        assertThat(logWithoutItem.getCompletedItems()).contains("exercise");
    }

    @Test
    void shouldBeIdempotent_whenAddingItemAlreadyPresent() {
        when(routineRepository.findByUserAndDate(user, date)).thenReturn(Optional.of(routineLog));
        when(routineRepository.save(routineLog)).thenReturn(routineLog);
        when(mapper.toView(routineLog)).thenReturn(view);

        commandService.addItem(user, date, new AddRoutineItemCommand("exercise"));

        assertThat(routineLog.getCompletedItems()).containsExactly("exercise");
    }

    @Test
    void shouldRemoveItem_whenItemPresent() {
        when(routineRepository.findByUserAndDate(user, date)).thenReturn(Optional.of(routineLog));
        when(routineRepository.save(routineLog)).thenReturn(routineLog);
        when(mapper.toView(routineLog)).thenReturn(view);

        commandService.removeItem(user, date, new RemoveRoutineItemCommand("exercise"));

        assertThat(routineLog.getCompletedItems()).doesNotContain("exercise");
    }

    @Test
    void shouldBeIdempotent_whenRemovingItemNotPresent() {
        when(routineRepository.findByUserAndDate(user, date)).thenReturn(Optional.of(routineLog));
        when(routineRepository.save(routineLog)).thenReturn(routineLog);
        when(mapper.toView(routineLog)).thenReturn(view);

        commandService.removeItem(user, date, new RemoveRoutineItemCommand("meditation"));

        assertThat(routineLog.getCompletedItems()).containsExactly("exercise");
        verify(routineRepository).save(routineLog);
    }

    @Test
    void shouldReplaceItems_whenSetItemsCalled() {
        when(routineRepository.findByUserAndDate(user, date)).thenReturn(Optional.of(routineLog));
        when(routineRepository.save(routineLog)).thenReturn(routineLog);
        when(mapper.toView(routineLog)).thenReturn(view);

        commandService.setItems(user, date, new SetRoutineItemsCommand(List.of("meditation", "reading")));

        assertThat(routineLog.getCompletedItems()).containsExactly("meditation", "reading");
    }

    @Test
    void shouldDeleteLog_whenLogExists() {
        when(routineRepository.findByUserAndDate(user, date)).thenReturn(Optional.of(routineLog));

        commandService.deleteLog(user, date);

        verify(routineRepository).delete(routineLog);
    }

    @Test
    void shouldThrowResourceNotFoundException_whenDeletingNonExistentLog() {
        when(routineRepository.findByUserAndDate(user, date)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> commandService.deleteLog(user, date))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(routineRepository, never()).delete(any());
    }
}
