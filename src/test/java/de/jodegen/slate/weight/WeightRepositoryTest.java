package de.jodegen.slate.weight;

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
class WeightRepositoryTest {

    @Autowired WeightRepository weightRepository;
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
        em.persistAndFlush(WeightEntry.builder().user(user).date(LocalDate.of(2026, 5, 12)).kg(79.0).build());
        em.persistAndFlush(WeightEntry.builder().user(user).date(LocalDate.of(2026, 5, 14)).kg(80.0).build());
        em.persistAndFlush(WeightEntry.builder().user(user).date(LocalDate.of(2026, 5, 13)).kg(79.5).build());

        var results = weightRepository.findByUserOrderByDateDesc(user);

        assertThat(results).hasSize(3);
        assertThat(results.get(0).getDate()).isEqualTo(LocalDate.of(2026, 5, 14));
        assertThat(results.get(2).getDate()).isEqualTo(LocalDate.of(2026, 5, 12));
    }

    @Test
    void shouldOnlyReturnEntriesForGivenUser() {
        em.persistAndFlush(WeightEntry.builder().user(user).date(LocalDate.of(2026, 5, 14)).kg(80.0).build());
        em.persistAndFlush(WeightEntry.builder().user(otherUser).date(LocalDate.of(2026, 5, 14)).kg(70.0).build());

        assertThat(weightRepository.findByUserOrderByDateDesc(user)).hasSize(1);
    }

    @Test
    void shouldDetectExistingEntryByUserAndDate() {
        LocalDate date = LocalDate.of(2026, 5, 14);
        em.persistAndFlush(WeightEntry.builder().user(user).date(date).kg(80.0).build());

        assertThat(weightRepository.existsByUserAndDate(user, date)).isTrue();
        assertThat(weightRepository.existsByUserAndDate(otherUser, date)).isFalse();
    }

    @Test
    void shouldEnforceUniqueConstraint_perUserAndDate() {
        LocalDate date = LocalDate.of(2026, 5, 14);
        em.persistAndFlush(WeightEntry.builder().user(user).date(date).kg(80.0).build());

        org.junit.jupiter.api.Assertions.assertThrows(Exception.class, () -> {
            em.persistAndFlush(WeightEntry.builder().user(user).date(date).kg(81.0).build());
        });
    }
}
