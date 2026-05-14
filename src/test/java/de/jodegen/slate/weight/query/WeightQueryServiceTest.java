package de.jodegen.slate.weight.query;

import de.jodegen.slate.user.User;
import de.jodegen.slate.weight.WeightEntry;
import de.jodegen.slate.weight.WeightRepository;
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
class WeightQueryServiceTest {

    @Mock WeightRepository weightRepository;
    @Mock WeightEntryMapper mapper;
    @InjectMocks WeightQueryService queryService;

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder().id(UUID.randomUUID()).email("test@test.com").name("Test").build();
    }

    @Test
    void shouldReturnAllEntries_orderedByDateDesc() {
        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);
        WeightEntry e1 = WeightEntry.builder().id(UUID.randomUUID()).user(user).date(today).kg(80.0).createdAt(Instant.now()).build();
        WeightEntry e2 = WeightEntry.builder().id(UUID.randomUUID()).user(user).date(yesterday).kg(79.5).createdAt(Instant.now()).build();

        WeightEntryView v1 = new WeightEntryView(e1.getId(), today, 80.0, e1.getCreatedAt());
        WeightEntryView v2 = new WeightEntryView(e2.getId(), yesterday, 79.5, e2.getCreatedAt());

        when(weightRepository.findByUserOrderByDateDesc(user)).thenReturn(List.of(e1, e2));
        when(mapper.toView(e1)).thenReturn(v1);
        when(mapper.toView(e2)).thenReturn(v2);

        List<WeightEntryView> result = queryService.findAll(user);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).date()).isEqualTo(today);
        assertThat(result.get(1).date()).isEqualTo(yesterday);
    }

    @Test
    void shouldReturnEmptyList_whenNoEntries() {
        when(weightRepository.findByUserOrderByDateDesc(user)).thenReturn(List.of());

        assertThat(queryService.findAll(user)).isEmpty();
    }
}
