package de.jodegen.slate.routine.query;

import de.jodegen.slate.common.exception.ResourceNotFoundException;
import de.jodegen.slate.routine.RoutineLog;
import de.jodegen.slate.routine.RoutineRepository;
import de.jodegen.slate.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoutineQueryServiceTest {

    @Mock RoutineRepository routineRepository;
    @Mock RoutineLogMapper mapper;
    @InjectMocks RoutineQueryService queryService;

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder().id(UUID.randomUUID()).email("test@test.com").name("Test").build();
    }

    @Test
    void shouldReturnAllLogs_mappedToViews() {
        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);

        RoutineLog log1 = RoutineLog.builder().id(UUID.randomUUID()).user(user).date(today)
                .completedItems(List.of("exercise")).createdAt(Instant.now()).build();
        RoutineLog log2 = RoutineLog.builder().id(UUID.randomUUID()).user(user).date(yesterday)
                .completedItems(List.of()).createdAt(Instant.now()).build();

        RoutineLogView view1 = new RoutineLogView(log1.getId(), today, log1.getCompletedItems(), log1.getCreatedAt());
        RoutineLogView view2 = new RoutineLogView(log2.getId(), yesterday, log2.getCompletedItems(), log2.getCreatedAt());

        when(routineRepository.findByUserOrderByDateDesc(user)).thenReturn(List.of(log1, log2));
        when(mapper.toView(log1)).thenReturn(view1);
        when(mapper.toView(log2)).thenReturn(view2);

        List<RoutineLogView> result = queryService.findAll(user);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).date()).isEqualTo(today);
        assertThat(result.get(1).date()).isEqualTo(yesterday);
    }

    @Test
    void shouldReturnEmptyList_whenNoLogs() {
        when(routineRepository.findByUserOrderByDateDesc(user)).thenReturn(List.of());

        assertThat(queryService.findAll(user)).isEmpty();
    }

    @Test
    void shouldReturnView_whenLogFoundByDate() {
        LocalDate date = LocalDate.of(2026, 5, 14);
        RoutineLog log = RoutineLog.builder().id(UUID.randomUUID()).user(user).date(date)
                .completedItems(List.of("exercise")).createdAt(Instant.now()).build();
        RoutineLogView view = new RoutineLogView(log.getId(), date, log.getCompletedItems(), log.getCreatedAt());

        when(routineRepository.findByUserAndDate(user, date)).thenReturn(Optional.of(log));
        when(mapper.toView(log)).thenReturn(view);

        assertThat(queryService.findByDate(user, date)).isEqualTo(view);
    }

    @Test
    void shouldThrowResourceNotFoundException_whenLogNotFoundByDate() {
        LocalDate date = LocalDate.of(2026, 5, 14);
        when(routineRepository.findByUserAndDate(user, date)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> queryService.findByDate(user, date))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
