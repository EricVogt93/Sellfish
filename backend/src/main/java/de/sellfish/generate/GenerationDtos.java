package de.sellfish.generate;

import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.UUID;

public final class GenerationDtos {

    private GenerationDtos() {
    }

    public record GenerateRequest(
            @NotNull UUID jobMatchId,
            @NotNull GenerationType type) {
    }

    public record UpdateRequest(String content) {
    }

    public record GeneratedResponse(
            UUID id,
            UUID jobMatchId,
            GenerationType type,
            String content,
            String model,
            String promptVersion,
            int version,
            Instant createdAt) {

        public static GeneratedResponse from(GeneratedDocument d) {
            return new GeneratedResponse(d.getId(), d.getJobMatchId(), d.getType(), d.getContent(),
                    d.getModel(), d.getPromptVersion(), d.getVersion(), d.getCreatedAt());
        }
    }
}
