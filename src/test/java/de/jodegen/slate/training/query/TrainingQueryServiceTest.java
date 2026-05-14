package de.jodegen.slate.training.query;

import de.jodegen.slate.common.DataSource;
import de.jodegen.slate.common.exception.ResourceNotFoundException;
import de.jodegen.slate.training.TrainingDay;
import de.jodegen.slate.training.TrainingDayRepository;
import de.jodegen.slate.training.TrainingSession;
import de.jodegen.slate.training.TrainingType;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TrainingQueryServiceTest {

    @Mock TrainingDayRepository trainingDayRepository;
    @Mock TrainingDayMapper trainingDayMapper;
    @InjectMocks TrainingQueryService queryService;

    private User user;
    private LocalDate date;
    private TrainingDay day;
    private TrainingDayView dayView;
    private TrainingDaySummaryView summaryView;

    @BeforeEach
    void setUp() {
        user = User.builder().id(UUID.randomUUID()).email("test@test.com").name("Test").build();
        date = LocalDate.of(2026, 5, 14);

        TrainingSession session = TrainingSession.builder()
                .id(UUID.randomUUID()).type(TrainingType.PUSH).source(DataSource.MANUAL)
                .sets(new ArrayList<>()).build();

        day = TrainingDay.builder()
                .id(UUID.randomUUID()).user(user).date(date).plannedType(TrainingType.PUSH)
                .createdAt(Instant.now()).sessions(new ArrayList<>(List.of(session))).build();

        dayView = new TrainingDayView(day.getId(), date, TrainingType.PUSH,
                List.of(new TrainingSessionView(session.getId(), TrainingType.PUSH, null, DataSource.MANUAL, List.of(), Instant.now())),
                day.getCreatedAt());

        summaryView = new TrainingDaySummaryView(day.getId(), date, TrainingType.PUSH, 1, day.getCreatedAt());
    }

    @Test
    void findAll_returnsSummaryViews() {
        when(trainingDayRepository.findByUserOrderByDateDesc(user)).thenReturn(List.of(day));
        when(trainingDayMapper.toSummaryViewList(List.of(day))).thenReturn(List.of(summaryView));

        List<TrainingDaySummaryView> result = queryService.findAll(user);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).sessionCount()).isEqualTo(1);
    }

    @Test
    void findByDate_returnsFullView() {
        when(trainingDayRepository.findByUserAndDate(user, date)).thenReturn(Optional.of(day));
        when(trainingDayMapper.toView(day)).thenReturn(dayView);

        TrainingDayView result = queryService.findByDate(user, date);

        assertThat(result).isEqualTo(dayView);
        assertThat(result.sessions()).hasSize(1);
    }

    @Test
    void findByDate_notFound_throwsResourceNotFoundException() {
        when(trainingDayRepository.findByUserAndDate(user, date)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> queryService.findByDate(user, date))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
