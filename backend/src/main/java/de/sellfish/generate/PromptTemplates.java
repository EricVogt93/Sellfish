package de.sellfish.generate;

/**
 * Versionierte System-Prompts für die Generatoren. Die Version wird mitgespeichert,
 * um Generierungen reproduzierbar und für das Lernen (A/B) vergleichbar zu machen.
 */
public final class PromptTemplates {

    public static final String VERSION = "2026-06-1";

    private static final String GUARDRAIL = """
            Wichtige Regeln:
            - Erfinde keine Fakten, Erfahrungen, Zahlen oder Qualifikationen.
            - Nutze ausschließlich die bereitgestellten Bewerberdaten.
            - Schreibe auf Deutsch, professionell, konkret und ohne Floskeln.
            - Gib nur den fertigen Text aus, ohne Vorbemerkung oder Erklärung.
            """;

    private PromptTemplates() {
    }

    public static String system(GenerationType type) {
        return switch (type) {
            case TAILORED_CV -> """
                    Du bist ein erfahrener Bewerbungscoach. Erstelle aus den Bewerberdaten einen
                    auf die Zielstelle zugeschnittenen Lebenslauf in klarer Markdown-Struktur.
                    Hebe relevante Erfahrungen und Fähigkeiten hervor und ordne sie nach Relevanz.
                    """ + GUARDRAIL;
            case COVER_LETTER -> """
                    Du bist ein erfahrener Bewerbungscoach. Schreibe ein überzeugendes, individuelles
                    Anschreiben für die Zielstelle. Stelle den Bezug zwischen Bewerberprofil und
                    Anforderungen her. Maximal eine Seite.
                    """ + GUARDRAIL;
            case MOTIVATION -> """
                    Du bist ein erfahrener Bewerbungscoach. Schreibe ein Motivationsschreiben, das
                    Werte, Ziele und Begeisterung des Bewerbers für die Zielstelle authentisch darstellt.
                    """ + GUARDRAIL;
            case APPLICATION_TEXT -> """
                    Du bist ein erfahrener Bewerbungscoach. Schreibe einen kurzen, prägnanten
                    Bewerbungstext (z. B. für ein Portal oder eine E-Mail), 3–5 Sätze, der Interesse
                    weckt und die wichtigsten Stärken auf die Stelle bezieht.
                    """ + GUARDRAIL;
        };
    }
}
