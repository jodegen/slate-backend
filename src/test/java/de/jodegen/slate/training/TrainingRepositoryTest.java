package de.jodegen.slate.training;

import de.jodegen.slate.common.DataSource;
import de.jodegen.slate.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DataJpaTest
@TestPropertySource(properties = {
        "spring.liquibase.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class TrainingRepositoryTest {

    @Autowired TrainingDayRepository trainingDayRepository;
    @Autowired TestEntityManager em;

    private User user;
    private User otherUser;

    @BeforeEach
    void setUp() {
        user = em.persistAndFlush(User.builder()
                .email("a@test.com").name("Alice").passwordHash("hash").build());
        otherUser = em.persistAndFlush(User.builder()
                .email("b@test.com").name("Bob").passwordHash("hash").build());
    }

    @Test
    void shouldReturnDaysOrderedByDateDesc() {
        em.persistAndFlush(TrainingDay.builder().user(user).date(LocalDate.of(2026, 5, 12)).plannedType(TrainingType.PUSH).sessions(new ArrayList<>()).build());
        em.persistAndFlush(TrainingDay.builder().user(user).date(LocalDate.of(2026, 5, 14)).plannedType(TrainingType.PULL).sessions(new ArrayList<>()).build());
        em.persistAndFlush(TrainingDay.builder().user(user).date(LocalDate.of(2026, 5, 13)).plannedType(TrainingType.CARDIO).sessions(new ArrayList<>()).build());

        List<TrainingDay> results = trainingDayRepository.findByUserOrderByDateDesc(user);

        assertThat(results).hasSize(3);
        assertThat(results.get(0).getDate()).isEqualTo(LocalDate.of(2026, 5, 14));
        assertThat(results.get(2).getDate()).isEqualTo(LocalDate.of(2026, 5, 12));
    }

    @Test
    void findByUserAndDate_foundAndNotFound() {
        LocalDate date = LocalDate.of(2026, 5, 14);
        em.persistAndFlush(TrainingDay.builder().user(user).date(date).plannedType(TrainingType.PUSH).sessions(new ArrayList<>()).build());

        assertThat(trainingDayRepository.findByUserAndDate(user, date)).isPresent();
        assertThat(trainingDayRepository.findByUserAndDate(otherUser, date)).isEmpty();
        assertThat(trainingDayRepository.findByUserAndDate(user, date.plusDays(1))).isEmpty();
    }

    @Test
    void shouldEnforceUniqueConstraint_perUserAndDate() {
        LocalDate date = LocalDate.of(2026, 5, 14);
        em.persistAndFlush(TrainingDay.builder().user(user).date(date).plannedType(TrainingType.PUSH).sessions(new ArrayList<>()).build());

        assertThrows(Exception.class, () ->
                em.persistAndFlush(TrainingDay.builder().user(user).date(date).plannedType(TrainingType.PULL).sessions(new ArrayList<>()).build())
        );
    }
}
