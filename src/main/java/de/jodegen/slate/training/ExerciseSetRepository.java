package de.jodegen.slate.training;

import de.jodegen.slate.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ExerciseSetRepository extends JpaRepository<ExerciseSet, UUID> {
    List<ExerciseSet> findBySession(TrainingSession session);
    Optional<ExerciseSet> findByIdAndSessionTrainingDayUser(UUID id, User user);
}
