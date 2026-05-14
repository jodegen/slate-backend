package de.jodegen.slate.routine;

import de.jodegen.slate.auth.CustomUserDetailsService;
import de.jodegen.slate.auth.JwtService;
import de.jodegen.slate.common.exception.ResourceNotFoundException;
import de.jodegen.slate.routine.command.AddRoutineItemCommand;
import de.jodegen.slate.routine.command.RemoveRoutineItemCommand;
import de.jodegen.slate.routine.command.RoutineCommandService;
import de.jodegen.slate.routine.command.SetRoutineItemsCommand;
import de.jodegen.slate.routine.query.RoutineLogView;
import de.jodegen.slate.routine.query.RoutineQueryService;
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

@WebMvcTest(RoutineController.class)
class RoutineControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean RoutineCommandService commandService;
    @MockitoBean RoutineQueryService queryService;
    @MockitoBean JwtService jwtService;
    @MockitoBean UserRepository userRepository;
    @MockitoBean CustomUserDetailsService customUserDetailsService;

    private User mockUser;
    private RoutineLogView sampleView;
    private final LocalDate testDate = LocalDate.of(2026, 5, 14);

    @BeforeEach
    void setUp() {
        mockUser = User.builder()
                .id(UUID.randomUUID())
                .email("test@test.com")
                .name("Test User")
                .build();
        sampleView = new RoutineLogView(UUID.randomUUID(), testDate, List.of("exercise", "reading"), Instant.now());
    }

    @Test
    void shouldReturn200WithLogs_whenGetAll() throws Exception {
        when(queryService.findAll(mockUser)).thenReturn(List.of(sampleView));

        mockMvc.perform(get("/api/routines").with(user(mockUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].completedItems[0]").value("exercise"));
    }

    @Test
    void shouldReturn200_whenGetByDate() throws Exception {
        when(queryService.findByDate(mockUser, testDate)).thenReturn(sampleView);

        mockMvc.perform(get("/api/routines/2026-05-14").with(user(mockUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completedItems[0]").value("exercise"));
    }

    @Test
    void shouldReturn404_whenGetByDateNotFound() throws Exception {
        when(queryService.findByDate(any(), eq(testDate)))
                .thenThrow(new ResourceNotFoundException("Not found"));

        mockMvc.perform(get("/api/routines/2026-05-14").with(user(mockUser)))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturn200_whenAddItem() throws Exception {
        when(commandService.addItem(any(), eq(testDate), any(AddRoutineItemCommand.class))).thenReturn(sampleView);

        mockMvc.perform(post("/api/routines/2026-05-14/items")
                        .with(user(mockUser)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"item":"exercise"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completedItems[0]").value("exercise"));
    }

    @Test
    void shouldReturn400_whenAddItemWithBlankItem() throws Exception {
        mockMvc.perform(post("/api/routines/2026-05-14/items")
                        .with(user(mockUser)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"item":""}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn200_whenRemoveItem() throws Exception {
        when(commandService.removeItem(any(), eq(testDate), any(RemoveRoutineItemCommand.class))).thenReturn(sampleView);

        mockMvc.perform(delete("/api/routines/2026-05-14/items")
                        .with(user(mockUser)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"item":"exercise"}
                                """))
                .andExpect(status().isOk());
    }

    @Test
    void shouldReturn200_whenSetItems() throws Exception {
        when(commandService.setItems(any(), eq(testDate), any(SetRoutineItemsCommand.class))).thenReturn(sampleView);

        mockMvc.perform(put("/api/routines/2026-05-14/items")
                        .with(user(mockUser)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"items":["exercise","reading"]}
                                """))
                .andExpect(status().isOk());
    }

    @Test
    void shouldReturn204_whenDeleteLog() throws Exception {
        mockMvc.perform(delete("/api/routines/2026-05-14")
                        .with(user(mockUser)).with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test
    void shouldReturn401_whenNotAuthenticated() throws Exception {
        mockMvc.perform(get("/api/routines"))
                .andExpect(status().isUnauthorized());
    }
}
