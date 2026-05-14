package de.jodegen.slate.training;

import de.jodegen.slate.auth.CustomUserDetailsService;
import de.jodegen.slate.auth.JwtService;
import de.jodegen.slate.common.DataSource;
import de.jodegen.slate.common.exception.ResourceNotFoundException;
import de.jodegen.slate.training.command.TrainingCommandService;
import de.jodegen.slate.training.query.TrainingDaySummaryView;
import de.jodegen.slate.training.query.TrainingDayView;
import de.jodegen.slate.training.query.TrainingQueryService;
import de.jodegen.slate.training.query.TrainingSessionView;
import de.jodegen.slate.user.User;
import de.jodegen.slate.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TrainingController.class)
class TrainingControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean TrainingCommandService commandService;
    @MockitoBean TrainingQueryService queryService;
    @MockitoBean JwtService jwtService;
    @MockitoBean UserRepository userRepository;
    @MockitoBean CustomUserDetailsService customUserDetailsService;

    private User mockUser;
    private UUID sessionId;
    private LocalDate date;
    private TrainingDayView dayView;
    private TrainingDaySummaryView summaryView;

    @BeforeEach
    void setUp() {
        mockUser = User.builder()
                .id(UUID.randomUUID())
                .email("test@test.com")
                .name("Test User")
                .build();
        sessionId = UUID.randomUUID();
        date = LocalDate.of(2026, 5, 14);

        TrainingSessionView sessionView = new TrainingSessionView(
                sessionId, TrainingType.PUSH, 60, DataSource.MANUAL, List.of(), Instant.now());
        dayView = new TrainingDayView(UUID.randomUUID(), date, TrainingType.PUSH, List.of(sessionView), Instant.now());
        summaryView = new TrainingDaySummaryView(UUID.randomUUID(), date, TrainingType.PUSH, 1, Instant.now());
    }

    @Test
    void shouldReturn200WithList_whenGetAll() throws Exception {
        when(queryService.findAll(mockUser)).thenReturn(List.of(summaryView));

        mockMvc.perform(get("/api/training").with(user(mockUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].plannedType").value("PUSH"));
    }

    @Test
    void shouldReturn200WithDay_whenGetByDate() throws Exception {
        when(queryService.findByDate(mockUser, date)).thenReturn(dayView);

        mockMvc.perform(get("/api/training/2026-05-14").with(user(mockUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.plannedType").value("PUSH"));
    }

    @Test
    void shouldReturn201_whenCreateDay() throws Exception {
        when(commandService.createDay(any(), any())).thenReturn(dayView);

        mockMvc.perform(post("/api/training")
                        .with(user(mockUser)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"date":"2026-05-14","plannedType":"PUSH"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.plannedType").value("PUSH"));
    }

    @Test
    void shouldReturn400_whenCreateDayWithInvalidBody() throws Exception {
        mockMvc.perform(post("/api/training")
                        .with(user(mockUser)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"date":"2026-05-14"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn204_whenDeleteDay() throws Exception {
        mockMvc.perform(delete("/api/training/2026-05-14")
                        .with(user(mockUser)).with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test
    void shouldReturn200_whenAddSession() throws Exception {
        when(commandService.addSession(any(), eq(date), any())).thenReturn(dayView);

        mockMvc.perform(post("/api/training/2026-05-14/sessions")
                        .with(user(mockUser)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"type":"PULL","durationMinutes":45}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.plannedType").value("PUSH"));
    }

    @Test
    void shouldReturn200_whenUpdateSession() throws Exception {
        when(commandService.updateSession(any(), eq(date), eq(sessionId), any())).thenReturn(dayView);

        mockMvc.perform(patch("/api/training/2026-05-14/sessions/" + sessionId)
                        .with(user(mockUser)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"type":"CARDIO","durationMinutes":30}
                                """))
                .andExpect(status().isOk());
    }

    @Test
    void shouldReturn200_whenDeleteSession() throws Exception {
        when(commandService.deleteSession(any(), eq(date), eq(sessionId))).thenReturn(dayView);

        mockMvc.perform(delete("/api/training/2026-05-14/sessions/" + sessionId)
                        .with(user(mockUser)).with(csrf()))
                .andExpect(status().isOk());
    }

    @Test
    void shouldReturn200_whenAddSet() throws Exception {
        when(commandService.addSet(any(), eq(date), eq(sessionId), any())).thenReturn(dayView);

        mockMvc.perform(post("/api/training/2026-05-14/sessions/" + sessionId + "/sets")
                        .with(user(mockUser)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"exerciseName":"Bench Press","reps":10,"weightKg":80.0}
                                """))
                .andExpect(status().isOk());
    }

    @Test
    void shouldReturn401_whenNotAuthenticated() throws Exception {
        mockMvc.perform(get("/api/training"))
                .andExpect(status().isUnauthorized());
    }
}
