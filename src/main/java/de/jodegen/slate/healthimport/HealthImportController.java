package de.jodegen.slate.healthimport;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/health")
@RequiredArgsConstructor
@Slf4j
public class HealthImportController {

    @PostMapping("/import")
    public Map<String, String> importHealth(@RequestBody String rawBody) {
        log.info("Health import raw payload:\n{}", rawBody);
        return Map.of("status", "received");
    }
}
