package de.jodegen.slate.weight.command;

import de.jodegen.slate.common.exception.ConflictException;
import de.jodegen.slate.common.exception.ResourceNotFoundException;
import de.jodegen.slate.user.User;
import de.jodegen.slate.weight.WeightEntry;
import de.jodegen.slate.weight.WeightRepository;
import de.jodegen.slate.weight.query.WeightEntryMapper;
import de.jodegen.slate.weight.query.WeightEntryView;
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
class WeightCommandServiceTest {

    @Mock WeightRepository weightRepository;
    @Mock WeightEntryMapper mapper;
    @InjectMocks WeightCommandService commandService;

    private User user;
    private LocalDate date;
    private WeightEntry entry;
    private WeightEntryView view;

    @BeforeEach
    void setUp() {
        user = User.builder().id(UUID.randomUUID()).email("test@test.com").name("Test").build();
        date = LocalDate.of(2026, 5, 14);
        entry = WeightEntry.builder().id(UUID.randomUUID()).user(user).date(date).kg(80.0).createdAt(Instant.now()).build();
        view = new WeightEntryView(entry.getId(), date, 80.0, entry.getCreatedAt());
    }

    @Test
    void shouldCreateEntry_whenValidCommand() {
        when(weightRepository.existsByUserAndDate(user, date)).thenReturn(false);
        when(weightRepository.save(any())).thenReturn(entry);
        when(mapper.toView(entry)).thenReturn(view);

        WeightEntryView result = commandService.create(user, new CreateWeightCommand(date, 80.0));

        assertThat(result).isEqualTo(view);
        verify(weightRepository).save(any(WeightEntry.class));
    }

    @Test
    void shouldThrowConflict_whenEntryAlreadyExistsForDate() {
        when(weightRepository.existsByUserAndDate(user, date)).thenReturn(true);

        assertThatThrownBy(() -> commandService.create(user, new CreateWeightCommand(date, 80.0)))
                .isInstanceOf(ConflictException.class);

        verify(weightRepository, never()).save(any());
    }

    @Test
    void shouldUpdateExistingEntry_whenUpsertAndEntryExists() {
        when(weightRepository.findByUserAndDate(user, date)).thenReturn(Optional.of(entry));
        when(weightRepository.save(entry)).thenReturn(entry);
        when(mapper.toView(entry)).thenReturn(new WeightEntryView(entry.getId(), date, 85.0, entry.getCreatedAt()));

        WeightEntryView result = commandService.upsert(user, date, new UpdateWeightCommand(85.0));

        assertThat(result.kg()).isEqualTo(85.0);
        assertThat(entry.getKg()).isEqualTo(85.0);
    }

    @Test
    void shouldCreateNewEntry_whenUpsertAndNoExistingEntry() {
        when(weightRepository.findByUserAndDate(user, date)).thenReturn(Optional.empty());
        when(weightRepository.save(any())).thenReturn(entry);
        when(mapper.toView(entry)).thenReturn(view);

        WeightEntryView result = commandService.upsert(user, date, new UpdateWeightCommand(80.0));

        assertThat(result).isEqualTo(view);
        verify(weightRepository).save(any(WeightEntry.class));
    }

    @Test
    void shouldDelete_whenEntryExists() {
        when(weightRepository.findByUserAndDate(user, date)).thenReturn(Optional.of(entry));

        commandService.delete(user, date);

        verify(weightRepository).delete(entry);
    }

    @Test
    void shouldThrowNotFound_whenDeletingNonExistentEntry() {
        when(weightRepository.findByUserAndDate(user, date)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> commandService.delete(user, date))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(weightRepository, never()).delete(any());
    }
}
