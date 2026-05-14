package de.jodegen.slate.sleep.query;

import de.jodegen.slate.common.DataSource;
import de.jodegen.slate.sleep.SleepLog;
import de.jodegen.slate.sleep.SleepRepository;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SleepQueryServiceTest {

    @Mock SleepRepository sleepRepository;
    @Mock SleepLogMapper mapper;
    @InjectMocks SleepQueryService queryService;

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder().id(UUID.randomUUID()).email("test@test.com").name("Test").build();
    }

    @Test
    void shouldReturnAllEntries_orderedByDateDesc() {
        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);
        SleepLog e1 = SleepLog.builder().id(UUID.randomUUID()).user(user).date(today)
                .durationMinutes(480).source(DataSource.MANUAL).createdAt(Instant.now()).build();
        SleepLog e2 = SleepLog.builder().id(UUID.randomUUID()).user(user).date(yesterday)
                .durationMinutes(420).source(DataSource.MANUAL).createdAt(Instant.now()).build();

        SleepLogView v1 = new SleepLogView(e1.getId(), today, 480, DataSource.MANUAL, e1.getCreatedAt());
        SleepLogView v2 = new SleepLogView(e2.getId(), yesterday, 420, DataSource.MANUAL, e2.getCreatedAt());

        when(sleepRepository.findByUserOrderByDateDesc(user)).thenReturn(List.of(e1, e2));
        when(mapper.toView(e1)).thenReturn(v1);
        when(mapper.toView(e2)).thenReturn(v2);

        List<SleepLogView> result = queryService.findAll(user);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).date()).isEqualTo(today);
        assertThat(result.get(1).date()).isEqualTo(yesterday);
    }

    @Test
    void shouldReturnEmptyList_whenNoEntries() {
        when(sleepRepository.findByUserOrderByDateDesc(user)).thenReturn(List.of());

        assertThat(queryService.findAll(user)).isEmpty();
    }
}
