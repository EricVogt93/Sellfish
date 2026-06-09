package de.bewerbungsatze.docs;

import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.IOException;

/**
 * Extrahiert reinen Text aus hochgeladenen Dokumenten (PDF, DOCX, TXT, …) via Apache Tika.
 */
@Service
public class TextExtractionService {

    private final Tika tika;

    public TextExtractionService() {
        this.tika = new Tika();
        // Kein Limit der extrahierten Zeichenanzahl.
        this.tika.setMaxStringLength(-1);
    }

    public String extract(byte[] content) {
        try (var in = new ByteArrayInputStream(content)) {
            return tika.parseToString(in).strip();
        } catch (IOException | TikaException e) {
            throw new DocumentProcessingException("Textextraktion fehlgeschlagen: " + e.getMessage(), e);
        }
    }
}
