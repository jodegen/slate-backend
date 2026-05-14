package de.jodegen.slate.training.command;

import de.jodegen.slate.common.DataSource;
import de.jodegen.slate.common.exception.ConflictException;
import de.jodegen.slate.common.exception.ResourceNotFoundException;
import de.jodegen.slate.common.exception.ValidationException;
import de.jodegen.slate.training.ExerciseSet;
import de.jodegen.slate.training.TrainingDay;
import de.jodegen.slate.training.TrainingDayRepository;
import de.jodegen.slate.training.TrainingSession;
import de.jodegen.slate.training.query.TrainingDayMapper;
import de.jodegen.slate.training.query.TrainingDayView;
import de.jodegen.slate.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TrainingCommandService {

    private final TrainingDayRepository trainingDayRepository;
    private final TrainingDayMapper trainingDayMapper;

    @Transactional
    public TrainingDayView createDay(User user, CreateTrainingDayCommand cmd) {
        if (trainingDayRepository.existsByUserAndDate(user, cmd.date())) {
            throw new ConflictException("Training day already exists for " + cmd.date());
        }
        TrainingDay day = TrainingDay.builder()
                .user(user)
                .date(cmd.date())
                .plannedType(cmd.plannedType())
                .build();
        TrainingSession defaultSession = TrainingSession.builder()
                .trainingDay(day)
                .type(cmd.plannedType())
                .source(DataSource.MANUAL)
                .build();
        day.getSessions().add(defaultSession);
        return trainingDayMapper.toView(trainingDayRepository.save(day));
    }

    @Transactional
    public TrainingDayView addSession(User user, LocalDate date, AddSessionCommand cmd) {
        TrainingDay day = findDayOrThrow(user, date);
        TrainingSession session = TrainingSession.builder()
                .trainingDay(day)
                .type(cmd.type())
                .durationMinutes(cmd.durationMinutes())
                .source(cmd.source() != null ? cmd.source() : DataSource.MANUAL)
                .build();
        day.getSessions().add(session);
        return trainingDayMapper.toView(trainingDayRepository.save(day));
    }

    @Transactional
    public TrainingDayView updateSession(User user, LocalDate date, UUID sessionId, UpdateSessionCommand cmd) {
        TrainingDay day = findDayOrThrow(user, date);
        TrainingSession session = day.getSessions().stream()
                .filter(s -> s.getId().equals(sessionId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Session not found: " + sessionId));
        session.setType(cmd.type());
        session.setDurationMinutes(cmd.durationMinutes());
        return trainingDayMapper.toView(trainingDayRepository.save(day));
    }

    @Transactional
    public TrainingDayView deleteSession(User user, LocalDate date, UUID sessionId) {
        TrainingDay day = findDayOrThrow(user, date);
        TrainingSession session = day.getSessions().stream()
                .filter(s -> s.getId().equals(sessionId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Session not found: " + sessionId));
        if (day.getSessions().size() <= 1) {
            throw new ValidationException("Cannot delete the last session of a training day");
        }
        day.getSessions().remove(session);
        return trainingDayMapper.toView(trainingDayRepository.save(day));
    }

    @Transactional
    public TrainingDayView addSet(User user, LocalDate date, UUID sessionId, AddExerciseSetCommand cmd) {
        TrainingDay day = findDayOrThrow(user, date);
        TrainingSession session = day.getSessions().stream()
                .filter(s -> s.getId().equals(sessionId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Session not found: " + sessionId));
        ExerciseSet set = ExerciseSet.builder()
                .session(session)
                .exerciseName(cmd.exerciseName())
                .reps(cmd.reps())
                .weightKg(cmd.weightKg())
                .build();
        session.getSets().add(set);
        return trainingDayMapper.toView(trainingDayRepository.save(day));
    }

    @Transactional
    public TrainingDayView deleteSet(User user, LocalDate date, UUID sessionId, UUID setId) {
        TrainingDay day = findDayOrThrow(user, date);
        TrainingSession session = day.getSessions().stream()
                .filter(s -> s.getId().equals(sessionId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Session not found: " + sessionId));
        ExerciseSet set = session.getSets().stream()
                .filter(es -> es.getId().equals(setId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Set not found: " + setId));
        session.getSets().remove(set);
        return trainingDayMapper.toView(trainingDayRepository.save(day));
    }

    @Transactional
    public void deleteDay(User user, LocalDate date) {
        TrainingDay day = findDayOrThrow(user, date);
        trainingDayRepository.delete(day);
    }

    private TrainingDay findDayOrThrow(User user, LocalDate date) {
        return trainingDayRepository.findByUserAndDate(user, date)
                .orElseThrow(() -> new ResourceNotFoundException("No training day for " + date));
    }
}
