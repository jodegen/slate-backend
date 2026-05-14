package de.jodegen.slate.healthimport;

import de.jodegen.slate.healthimport.dto.HealthImportRequest;
import de.jodegen.slate.healthimport.dto.HealthImportResponse;
import de.jodegen.slate.user.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/health")
@RequiredArgsConstructor
public class HealthImportController {

    private final HealthImportService healthImportService;

    @PostMapping("/import")
    @ResponseStatus(HttpStatus.OK)
    public HealthImportResponse importHealth(
            @AuthenticationPrincipal User user,
            @RequestBody @Valid HealthImportRequest request
    ) {
        return healthImportService.processImport(user, request);
    }
}
