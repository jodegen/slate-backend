package de.jodegen.slate.weight;

import de.jodegen.slate.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WeightRepository extends JpaRepository<WeightEntry, UUID> {
    List<WeightEntry> findByUserOrderByDateDesc(User user);
    Optional<WeightEntry> findByUserAndDate(User user, LocalDate date);
    boolean existsByUserAndDate(User user, LocalDate date);
}
