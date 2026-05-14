package de.jodegen.slate.training;

import de.jodegen.slate.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TrainingSessionRepository extends JpaRepository<TrainingSession, UUID> {
    List<TrainingSession> findByTrainingDay(TrainingDay trainingDay);
    Optional<TrainingSession> findByIdAndTrainingDayUser(UUID id, User user);
}
