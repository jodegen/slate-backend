package de.jodegen.slate.user;

import java.time.Instant;
import java.util.UUID;

public record UserView(UUID id, String email, String name, Instant createdAt) {}
