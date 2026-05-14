package de.jodegen.slate.sleep;

import de.jodegen.slate.sleep.command.CreateSleepCommand;
import de.jodegen.slate.sleep.command.SleepCommandService;
import de.jodegen.slate.sleep.command.UpdateSleepCommand;
import de.jodegen.slate.sleep.query.SleepLogView;
import de.jodegen.slate.sleep.query.SleepQueryService;
import de.jodegen.slate.user.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/sleep")
@RequiredArgsConstructor
public class SleepController {

    private final SleepCommandService commandService;
    private final SleepQueryService queryService;

    @GetMapping
    public ResponseEntity<List<SleepLogView>> getAll(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(queryService.findAll(user));
    }

    @PostMapping
    public ResponseEntity<SleepLogView> create(@AuthenticationPrincipal User user,
                                                @Valid @RequestBody CreateSleepCommand command) {
        return ResponseEntity.status(HttpStatus.CREATED).body(commandService.create(user, command));
    }

    @PutMapping("/{date}")
    public ResponseEntity<SleepLogView> upsert(@AuthenticationPrincipal User user,
                                                @PathVariable LocalDate date,
                                                @Valid @RequestBody UpdateSleepCommand command) {
        return ResponseEntity.ok(commandService.upsert(user, date, command));
    }

    @DeleteMapping("/{date}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal User user,
                                       @PathVariable LocalDate date) {
        commandService.delete(user, date);
        return ResponseEntity.noContent().build();
    }
}
