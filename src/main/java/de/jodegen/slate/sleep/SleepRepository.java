package de.jodegen.slate.sleep;

import de.jodegen.slate.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SleepRepository extends JpaRepository<SleepLog, UUID> {
    List<SleepLog> findByUserOrderByDateDesc(User user);
    Optional<SleepLog> findByUserAndDate(User user, LocalDate date);
    boolean existsByUserAndDate(User user, LocalDate date);
}
