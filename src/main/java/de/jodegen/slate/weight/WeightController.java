package de.jodegen.slate.weight;

import de.jodegen.slate.user.User;
import de.jodegen.slate.weight.command.CreateWeightCommand;
import de.jodegen.slate.weight.command.UpdateWeightCommand;
import de.jodegen.slate.weight.command.WeightCommandService;
import de.jodegen.slate.weight.query.WeightEntryView;
import de.jodegen.slate.weight.query.WeightQueryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/weights")
@RequiredArgsConstructor
public class WeightController {

    private final WeightCommandService commandService;
    private final WeightQueryService queryService;

    @GetMapping
    public ResponseEntity<List<WeightEntryView>> getAll(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(queryService.findAll(user));
    }

    @PostMapping
    public ResponseEntity<WeightEntryView> create(@AuthenticationPrincipal User user,
                                                   @Valid @RequestBody CreateWeightCommand command) {
        return ResponseEntity.status(HttpStatus.CREATED).body(commandService.create(user, command));
    }

    @PutMapping("/{date}")
    public ResponseEntity<WeightEntryView> upsert(@AuthenticationPrincipal User user,
                                                   @PathVariable LocalDate date,
                                                   @Valid @RequestBody UpdateWeightCommand command) {
        return ResponseEntity.ok(commandService.upsert(user, date, command));
    }

    @DeleteMapping("/{date}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal User user,
                                       @PathVariable LocalDate date) {
        commandService.delete(user, date);
        return ResponseEntity.noContent().build();
    }
}
