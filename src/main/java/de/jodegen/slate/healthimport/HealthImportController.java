package de.jodegen.slate.healthimport;

import de.jodegen.slate.common.exception.ResourceNotFoundException;
import de.jodegen.slate.healthimport.dto.SleepImportRequest;
import de.jodegen.slate.healthimport.dto.SleepImportResponse;
import de.jodegen.slate.healthimport.dto.StepsImportRequest;
import de.jodegen.slate.healthimport.dto.StepsImportResponse;
import de.jodegen.slate.user.User;
import de.jodegen.slate.user.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/health/import")
@RequiredArgsConstructor
public class HealthImportController {

    private final HealthImportService healthImportService;
    private final UserRepository userRepository;

    @Value("${import.api-key}")
    private String importApiKey;

    @PostMapping("/steps")
    public ResponseEntity<StepsImportResponse> importSteps(
            @RequestHeader(value = "X-Import-Key", required = false) String apiKey,
            @RequestBody @Valid StepsImportRequest request) {
        validateApiKey(apiKey);
        return ResponseEntity.ok(healthImportService.processSteps(resolveUser(), request));
    }

    @PostMapping("/sleep")
    public ResponseEntity<SleepImportResponse> importSleep(
            @RequestHeader(value = "X-Import-Key", required = false) String apiKey,
            @RequestBody @Valid SleepImportRequest request) {
        validateApiKey(apiKey);
        return ResponseEntity.ok(healthImportService.processSleep(resolveUser(), request));
    }

    private void validateApiKey(String apiKey) {
        if (!importApiKey.equals(apiKey)) {
            throw new BadCredentialsException("Invalid or missing import API key");
        }
    }

    private User resolveUser() {
        return userRepository.findAll().stream().findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("No user found in database"));
    }
}
