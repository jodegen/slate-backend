package de.jodegen.slate.healthimport;

import de.jodegen.slate.auth.CustomUserDetailsService;
import de.jodegen.slate.auth.JwtService;
import de.jodegen.slate.healthimport.dto.HealthImportRequest;
import de.jodegen.slate.healthimport.dto.HealthImportResponse;
import de.jodegen.slate.user.User;
import de.jodegen.slate.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

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
    }

    @Test
    void shouldReturn200WithResult_whenValidRequest() throws Exception {
        when(healthImportService.processImport(any(User.class), any(HealthImportRequest.class)))
                .thenReturn(new HealthImportResponse(2, 3, 1, 1));

        mockMvc.perform(post("/api/health/import")
                        .with(user(mockUser)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "sentAt": "2026-05-15T08:30:00.000Z",
                                  "steps": [],
                                  "sleep": [],
                                  "workouts": []
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sleepUpserted").value(2))
                .andExpect(jsonPath("$.stepsProcessed").value(3))
                .andExpect(jsonPath("$.routineUpdated").value(1))
                .andExpect(jsonPath("$.workoutsCreated").value(1));
    }

    @Test
    void shouldReturn400_whenMissingRequiredFields() throws Exception {
        mockMvc.perform(post("/api/health/import")
                        .with(user(mockUser)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn401_whenUnauthenticated() throws Exception {
        mockMvc.perform(post("/api/health/import")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"sentAt":"2026-05-15T08:30:00.000Z"}
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturn200WithAllZeros_whenEmptyLists() throws Exception {
        when(healthImportService.processImport(any(User.class), any(HealthImportRequest.class)))
                .thenReturn(new HealthImportResponse(0, 0, 0, 0));

        mockMvc.perform(post("/api/health/import")
                        .with(user(mockUser)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "sentAt": "2026-05-15T08:30:00.000Z",
                                  "steps": [],
                                  "sleep": [],
                                  "workouts": []
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sleepUpserted").value(0))
                .andExpect(jsonPath("$.stepsProcessed").value(0))
                .andExpect(jsonPath("$.routineUpdated").value(0))
                .andExpect(jsonPath("$.workoutsCreated").value(0));
    }
}
