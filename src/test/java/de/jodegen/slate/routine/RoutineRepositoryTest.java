package de.jodegen.slate.routine;

import de.jodegen.slate.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DataJpaTest
@TestPropertySource(properties = {
        "spring.liquibase.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class RoutineRepositoryTest {

    @Autowired RoutineRepository routineRepository;
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
    void shouldFindByUserAndDate_whenExists() {
        LocalDate date = LocalDate.of(2026, 5, 14);
        em.persistAndFlush(RoutineLog.builder().user(user).date(date).completedItems(List.of("exercise")).build());

        assertThat(routineRepository.findByUserAndDate(user, date)).isPresent();
        assertThat(routineRepository.findByUserAndDate(otherUser, date)).isEmpty();
    }

    @Test
    void shouldReturnLogsOrderedByDateDesc() {
        em.persistAndFlush(RoutineLog.builder().user(user).date(LocalDate.of(2026, 5, 12)).completedItems(List.of()).build());
        em.persistAndFlush(RoutineLog.builder().user(user).date(LocalDate.of(2026, 5, 14)).completedItems(List.of("a")).build());
        em.persistAndFlush(RoutineLog.builder().user(user).date(LocalDate.of(2026, 5, 13)).completedItems(List.of("b")).build());

        List<RoutineLog> results = routineRepository.findByUserOrderByDateDesc(user);

        assertThat(results).hasSize(3);
        assertThat(results.get(0).getDate()).isEqualTo(LocalDate.of(2026, 5, 14));
        assertThat(results.get(2).getDate()).isEqualTo(LocalDate.of(2026, 5, 12));
    }

    @Test
    void shouldEnforceUniqueConstraint_perUserAndDate() {
        LocalDate date = LocalDate.of(2026, 5, 14);
        em.persistAndFlush(RoutineLog.builder().user(user).date(date).completedItems(List.of()).build());

        assertThrows(Exception.class, () ->
                em.persistAndFlush(RoutineLog.builder().user(user).date(date).completedItems(List.of("x")).build())
        );
    }
}
