package de.jodegen.slate.routine;

import de.jodegen.slate.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RoutineRepository extends JpaRepository<RoutineLog, UUID> {
    Optional<RoutineLog> findByUserAndDate(User user, LocalDate date);
    List<RoutineLog> findByUserOrderByDateDesc(User user);
}
