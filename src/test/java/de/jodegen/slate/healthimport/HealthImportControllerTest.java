package de.jodegen.slate.healthimport;

import de.jodegen.slate.auth.CustomUserDetailsService;
import de.jodegen.slate.auth.JwtService;
import de.jodegen.slate.healthimport.dto.SleepImportResponse;
import de.jodegen.slate.healthimport.dto.StepsImportResponse;
import de.jodegen.slate.user.User;
import de.jodegen.slate.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(HealthImportController.class)
class HealthImportControllerTest {

    // The default key from application.yml: ${IMPORT_API_KEY:dev-import-key}
    private static final String VALID_KEY = "dev-import-key";

    @Autowired MockMvc mockMvc;
    @MockitoBean HealthImportService healthImportService;
    @MockitoBean JwtService jwtService;
    @MockitoBean UserRepository userRepository;
    @MockitoBean CustomUserDetailsService customUserDetailsService;

    private User mockUser;

    @BeforeEach
    void setUp() {
        mockUser = User.builder()
                .id(UUID.randomUUID())
                .email("test@test.com")
                .name("Test User")
                .build();
        when(userRepository.findAll()).thenReturn(List.of(mockUser));
    }

    @Test
    void steps_shouldReturn200_whenValidKeyAndPayload() throws Exception {
        when(healthImportService.processSteps(any(User.class), any()))
                .thenReturn(new StepsImportResponse(10500, true));

        mockMvc.perform(post("/api/health/import/steps").with(user(mockUser)).with(csrf())
                        .header("X-Import-Key", VALID_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "steps": "5000\\n5500",
                                  "dateTimes": "2026-05-14T08:00:00+02:00\\n2026-05-14T10:00:00+02:00"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalSteps").value(10500))
                .andExpect(jsonPath("$.routineUpdated").value(true));
    }

    @Test
    void steps_shouldReturn401_whenMissingApiKey() throws Exception {
        mockMvc.perform(post("/api/health/import/steps").with(user(mockUser)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"steps": "1000", "dateTimes": "2026-05-14T08:00:00+02:00"}
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void steps_shouldReturn401_whenWrongApiKey() throws Exception {
        mockMvc.perform(post("/api/health/import/steps").with(user(mockUser)).with(csrf())
                        .header("X-Import-Key", "wrong-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"steps": "1000", "dateTimes": "2026-05-14T08:00:00+02:00"}
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void steps_shouldReturn400_whenMissingRequiredFields() throws Exception {
        mockMvc.perform(post("/api/health/import/steps").with(user(mockUser)).with(csrf())
                        .header("X-Import-Key", VALID_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void sleep_shouldReturn200_whenValidKeyAndPayload() throws Exception {
        when(healthImportService.processSleep(any(User.class), any()))
                .thenReturn(new SleepImportResponse(LocalDate.of(2026, 5, 14), 420, true));

        mockMvc.perform(post("/api/health/import/sleep").with(user(mockUser)).with(csrf())
                        .header("X-Import-Key", VALID_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "sleepStartTimes": "2026-05-14T00:00:00+02:00",
                                  "sleepEndTimes": "2026-05-14T07:00:00+02:00",
                                  "sleepPhases": "Core"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.durationMinutes").value(420))
                .andExpect(jsonPath("$.upserted").value(true));
    }

    @Test
    void sleep_shouldReturn401_whenMissingApiKey() throws Exception {
        mockMvc.perform(post("/api/health/import/sleep").with(user(mockUser)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"sleepStartTimes": "...", "sleepEndTimes": "...", "sleepPhases": "Core"}
                                """))
                .andExpect(status().isUnauthorized());
    }
}
