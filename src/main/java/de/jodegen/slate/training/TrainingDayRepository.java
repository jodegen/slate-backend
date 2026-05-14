package de.jodegen.slate.training;

import de.jodegen.slate.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TrainingDayRepository extends JpaRepository<TrainingDay, UUID> {
    List<TrainingDay> findByUserOrderByDateDesc(User user);
    Optional<TrainingDay> findByUserAndDate(User user, LocalDate date);
    boolean existsByUserAndDate(User user, LocalDate date);
}
