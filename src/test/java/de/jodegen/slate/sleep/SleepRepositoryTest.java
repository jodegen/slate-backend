package de.jodegen.slate.sleep;

import de.jodegen.slate.common.DataSource;
import de.jodegen.slate.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@TestPropertySource(properties = {
        "spring.liquibase.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class SleepRepositoryTest {

    @Autowired SleepRepository sleepRepository;
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
    void shouldFindByUserOrderedByDateDesc() {
        em.persistAndFlush(SleepLog.builder().user(user).date(LocalDate.of(2026, 5, 12)).durationMinutes(420).source(DataSource.MANUAL).build());
        em.persistAndFlush(SleepLog.builder().user(user).date(LocalDate.of(2026, 5, 14)).durationMinutes(480).source(DataSource.MANUAL).build());
        em.persistAndFlush(SleepLog.builder().user(user).date(LocalDate.of(2026, 5, 13)).durationMinutes(450).source(DataSource.MANUAL).build());

        var results = sleepRepository.findByUserOrderByDateDesc(user);

        assertThat(results).hasSize(3);
        assertThat(results.get(0).getDate()).isEqualTo(LocalDate.of(2026, 5, 14));
        assertThat(results.get(2).getDate()).isEqualTo(LocalDate.of(2026, 5, 12));
    }

    @Test
    void shouldOnlyReturnEntriesForGivenUser() {
        em.persistAndFlush(SleepLog.builder().user(user).date(LocalDate.of(2026, 5, 14)).durationMinutes(480).source(DataSource.MANUAL).build());
        em.persistAndFlush(SleepLog.builder().user(otherUser).date(LocalDate.of(2026, 5, 14)).durationMinutes(360).source(DataSource.MANUAL).build());

        assertThat(sleepRepository.findByUserOrderByDateDesc(user)).hasSize(1);
    }

    @Test
    void shouldDetectExistingEntryByUserAndDate() {
        LocalDate date = LocalDate.of(2026, 5, 14);
        em.persistAndFlush(SleepLog.builder().user(user).date(date).durationMinutes(480).source(DataSource.MANUAL).build());

        assertThat(sleepRepository.existsByUserAndDate(user, date)).isTrue();
        assertThat(sleepRepository.existsByUserAndDate(otherUser, date)).isFalse();
    }

    @Test
    void shouldEnforceUniqueConstraint_perUserAndDate() {
        LocalDate date = LocalDate.of(2026, 5, 14);
        em.persistAndFlush(SleepLog.builder().user(user).date(date).durationMinutes(480).source(DataSource.MANUAL).build());

        org.junit.jupiter.api.Assertions.assertThrows(Exception.class, () -> {
            em.persistAndFlush(SleepLog.builder().user(user).date(date).durationMinutes(500).source(DataSource.MANUAL).build());
        });
    }
}
