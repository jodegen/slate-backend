package de.jodegen.slate.healthimport;

import de.jodegen.slate.healthimport.dto.HealthImportRequest;
import de.jodegen.slate.healthimport.dto.HealthImportResponse;
import de.jodegen.slate.user.User;
import de.jodegen.slate.user.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/health")
@RequiredArgsConstructor
@Slf4j
public class HealthImportController {

    private final HealthImportService healthImportService;
    private final UserRepository userRepository;

    @PostMapping("/import")
    @ResponseStatus(HttpStatus.OK)
    public HealthImportResponse importHealth(
            @RequestBody @Valid HealthImportRequest request
    ) {
        User user = userRepository.findAll().getFirst();
        log.info("Health import received for user {}: {}", user.getEmail(), request);
        return healthImportService.processImport(user, request);
    }
}
