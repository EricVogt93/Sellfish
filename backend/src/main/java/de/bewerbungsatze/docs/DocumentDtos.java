package de.bewerbungsatze.docs;

import java.time.Instant;
import java.util.UUID;

public final class DocumentDtos {

    private DocumentDtos() {
    }

    public record DocumentResponse(
            UUID id,
            DocumentType type,
            String filename,
            String mime,
            Long sizeBytes,
            boolean primary,
            boolean hasText,
            boolean hasStruct,
            Instant createdAt) {

        public static DocumentResponse from(Document d) {
            return new DocumentResponse(
                    d.getId(), d.getType(), d.getFilename(), d.getMime(), d.getSizeBytes(),
                    d.isPrimary(),
                    d.getParsedText() != null && !d.getParsedText().isBlank(),
                    d.getParsedStruct() != null && !d.getParsedStruct().isBlank(),
                    d.getCreatedAt());
        }
    }
}
