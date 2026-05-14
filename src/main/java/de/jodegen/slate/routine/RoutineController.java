package de.jodegen.slate.routine;

import de.jodegen.slate.routine.command.AddRoutineItemCommand;
import de.jodegen.slate.routine.command.RemoveRoutineItemCommand;
import de.jodegen.slate.routine.command.RoutineCommandService;
import de.jodegen.slate.routine.command.SetRoutineItemsCommand;
import de.jodegen.slate.routine.query.RoutineLogView;
import de.jodegen.slate.routine.query.RoutineQueryService;
import de.jodegen.slate.user.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/routines")
@RequiredArgsConstructor
public class RoutineController {

    private final RoutineCommandService commandService;
    private final RoutineQueryService queryService;

    @GetMapping
    public ResponseEntity<List<RoutineLogView>> getAll(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(queryService.findAll(user));
    }

    @GetMapping("/{date}")
    public ResponseEntity<RoutineLogView> getByDate(@AuthenticationPrincipal User user,
                                                    @PathVariable LocalDate date) {
        return ResponseEntity.ok(queryService.findByDate(user, date));
    }

    @PostMapping("/{date}/items")
    public ResponseEntity<RoutineLogView> addItem(@AuthenticationPrincipal User user,
                                                  @PathVariable LocalDate date,
                                                  @Valid @RequestBody AddRoutineItemCommand command) {
        return ResponseEntity.ok(commandService.addItem(user, date, command));
    }

    @DeleteMapping("/{date}/items")
    public ResponseEntity<RoutineLogView> removeItem(@AuthenticationPrincipal User user,
                                                     @PathVariable LocalDate date,
                                                     @Valid @RequestBody RemoveRoutineItemCommand command) {
        return ResponseEntity.ok(commandService.removeItem(user, date, command));
    }

    @PutMapping("/{date}/items")
    public ResponseEntity<RoutineLogView> setItems(@AuthenticationPrincipal User user,
                                                   @PathVariable LocalDate date,
                                                   @Valid @RequestBody SetRoutineItemsCommand command) {
        return ResponseEntity.ok(commandService.setItems(user, date, command));
    }

    @DeleteMapping("/{date}")
    public ResponseEntity<Void> deleteLog(@AuthenticationPrincipal User user,
                                          @PathVariable LocalDate date) {
        commandService.deleteLog(user, date);
        return ResponseEntity.noContent().build();
    }
}
