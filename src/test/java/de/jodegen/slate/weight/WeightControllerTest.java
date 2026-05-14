package de.jodegen.slate.weight;

import de.jodegen.slate.auth.CustomUserDetailsService;
import de.jodegen.slate.auth.JwtService;
import de.jodegen.slate.common.exception.ConflictException;
import de.jodegen.slate.common.exception.ResourceNotFoundException;
import de.jodegen.slate.user.User;
import de.jodegen.slate.user.UserRepository;
import de.jodegen.slate.weight.command.CreateWeightCommand;
import de.jodegen.slate.weight.command.UpdateWeightCommand;
import de.jodegen.slate.weight.command.WeightCommandService;
import de.jodegen.slate.weight.query.WeightEntryView;
import de.jodegen.slate.weight.query.WeightQueryService;
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

@WebMvcTest(WeightController.class)
class WeightControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean WeightCommandService commandService;
    @MockitoBean WeightQueryService queryService;
    @MockitoBean JwtService jwtService;
    @MockitoBean UserRepository userRepository;
    @MockitoBean CustomUserDetailsService customUserDetailsService;

    private User mockUser;
    private WeightEntryView sampleView;

    @BeforeEach
    void setUp() {
        mockUser = User.builder()
                .id(UUID.randomUUID())
                .email("test@test.com")
                .name("Test User")
                .build();
        sampleView = new WeightEntryView(UUID.randomUUID(), LocalDate.of(2026, 5, 14), 80.0, Instant.now());
    }

    @Test
    void shouldReturn200WithEntries_whenGetAll() throws Exception {
        when(queryService.findAll(mockUser)).thenReturn(List.of(sampleView));

        mockMvc.perform(get("/api/weights").with(user(mockUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].kg").value(80.0));
    }

    @Test
    void shouldReturn201_whenCreateSucceeds() throws Exception {
        when(commandService.create(any(), any(CreateWeightCommand.class))).thenReturn(sampleView);

        mockMvc.perform(post("/api/weights")
                        .with(user(mockUser)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"date":"2026-05-14","kg":80.0}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.kg").value(80.0));
    }

    @Test
    void shouldReturn400_whenCreateWithInvalidData() throws Exception {
        mockMvc.perform(post("/api/weights")
                        .with(user(mockUser)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"date":"2026-05-14","kg":-1.0}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn409_whenConflictOnCreate() throws Exception {
        when(commandService.create(any(), any(CreateWeightCommand.class)))
                .thenThrow(new ConflictException("Already exists"));

        mockMvc.perform(post("/api/weights")
                        .with(user(mockUser)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"date":"2026-05-14","kg":80.0}
                                """))
                .andExpect(status().isConflict());
    }

    @Test
    void shouldReturn200_whenUpsert() throws Exception {
        when(commandService.upsert(any(), any(LocalDate.class), any(UpdateWeightCommand.class)))
                .thenReturn(sampleView);

        mockMvc.perform(put("/api/weights/2026-05-14")
                        .with(user(mockUser)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"kg":82.5}
                                """))
                .andExpect(status().isOk());
    }

    @Test
    void shouldReturn204_whenDelete() throws Exception {
        mockMvc.perform(delete("/api/weights/2026-05-14").with(user(mockUser)).with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test
    void shouldReturn404_whenDeletingNonExistentEntry() throws Exception {
        doThrow(new ResourceNotFoundException("Not found"))
                .when(commandService).delete(any(), eq(LocalDate.of(2026, 5, 14)));

        mockMvc.perform(delete("/api/weights/2026-05-14").with(user(mockUser)).with(csrf()))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturn401_whenNotAuthenticated() throws Exception {
        mockMvc.perform(get("/api/weights"))
                .andExpect(status().isUnauthorized());
    }
}
