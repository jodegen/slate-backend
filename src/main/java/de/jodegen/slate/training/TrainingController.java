package de.jodegen.slate.training;

import de.jodegen.slate.training.command.AddExerciseSetCommand;
import de.jodegen.slate.training.command.AddSessionCommand;
import de.jodegen.slate.training.command.CreateTrainingDayCommand;
import de.jodegen.slate.training.command.TrainingCommandService;
import de.jodegen.slate.training.command.UpdateSessionCommand;
import de.jodegen.slate.training.query.TrainingDaySummaryView;
import de.jodegen.slate.training.query.TrainingDayView;
import de.jodegen.slate.training.query.TrainingQueryService;
import de.jodegen.slate.user.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/training")
@RequiredArgsConstructor
public class TrainingController {

    private final TrainingCommandService commandService;
    private final TrainingQueryService queryService;

    @GetMapping
    public ResponseEntity<List<TrainingDaySummaryView>> getAll(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(queryService.findAll(user));
    }

    @GetMapping("/{date}")
    public ResponseEntity<TrainingDayView> getByDate(@AuthenticationPrincipal User user,
                                                      @PathVariable LocalDate date) {
        return ResponseEntity.ok(queryService.findByDate(user, date));
    }

    @PostMapping
    public ResponseEntity<TrainingDayView> createDay(@AuthenticationPrincipal User user,
                                                      @Valid @RequestBody CreateTrainingDayCommand command) {
        return ResponseEntity.status(HttpStatus.CREATED).body(commandService.createDay(user, command));
    }

    @DeleteMapping("/{date}")
    public ResponseEntity<Void> deleteDay(@AuthenticationPrincipal User user,
                                          @PathVariable LocalDate date) {
        commandService.deleteDay(user, date);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{date}/sessions")
    public ResponseEntity<TrainingDayView> addSession(@AuthenticationPrincipal User user,
                                                       @PathVariable LocalDate date,
                                                       @Valid @RequestBody AddSessionCommand command) {
        return ResponseEntity.ok(commandService.addSession(user, date, command));
    }

    @PatchMapping("/{date}/sessions/{sessionId}")
    public ResponseEntity<TrainingDayView> updateSession(@AuthenticationPrincipal User user,
                                                          @PathVariable LocalDate date,
                                                          @PathVariable UUID sessionId,
                                                          @Valid @RequestBody UpdateSessionCommand command) {
        return ResponseEntity.ok(commandService.updateSession(user, date, sessionId, command));
    }

    @DeleteMapping("/{date}/sessions/{sessionId}")
    public ResponseEntity<TrainingDayView> deleteSession(@AuthenticationPrincipal User user,
                                                          @PathVariable LocalDate date,
                                                          @PathVariable UUID sessionId) {
        return ResponseEntity.ok(commandService.deleteSession(user, date, sessionId));
    }

    @PostMapping("/{date}/sessions/{sessionId}/sets")
    public ResponseEntity<TrainingDayView> addSet(@AuthenticationPrincipal User user,
                                                   @PathVariable LocalDate date,
                                                   @PathVariable UUID sessionId,
                                                   @Valid @RequestBody AddExerciseSetCommand command) {
        return ResponseEntity.ok(commandService.addSet(user, date, sessionId, command));
    }

    @DeleteMapping("/{date}/sessions/{sessionId}/sets/{setId}")
    public ResponseEntity<TrainingDayView> deleteSet(@AuthenticationPrincipal User user,
                                                      @PathVariable LocalDate date,
                                                      @PathVariable UUID sessionId,
                                                      @PathVariable UUID setId) {
        return ResponseEntity.ok(commandService.deleteSet(user, date, sessionId, setId));
    }
}
