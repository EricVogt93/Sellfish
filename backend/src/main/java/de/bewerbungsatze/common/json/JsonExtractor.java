package de.bewerbungsatze.common.json;

/**
 * Extrahiert das erste eingebettete JSON-Objekt oder -Array aus einem LLM-Text
 * (toleriert Markdown-Codefences und Begleittext).
 */
public final class JsonExtractor {

    private JsonExtractor() {
    }

    public static String extract(String raw) {
        if (raw == null) {
            return null;
        }
        String text = raw.strip();
        // Codefences entfernen.
        if (text.startsWith("```")) {
            int firstNewline = text.indexOf('\n');
            if (firstNewline > 0) {
                text = text.substring(firstNewline + 1);
            }
            int fenceEnd = text.lastIndexOf("```");
            if (fenceEnd >= 0) {
                text = text.substring(0, fenceEnd);
            }
            text = text.strip();
        }
        int objStart = text.indexOf('{');
        int arrStart = text.indexOf('[');
        int start;
        char open;
        char close;
        if (arrStart >= 0 && (objStart < 0 || arrStart < objStart)) {
            start = arrStart;
            open = '[';
            close = ']';
        } else if (objStart >= 0) {
            start = objStart;
            open = '{';
            close = '}';
        } else {
            return text;
        }

        int depth = 0;
        boolean inString = false;
        boolean escaped = false;
        for (int i = start; i < text.length(); i++) {
            char c = text.charAt(i);
            if (inString) {
                if (escaped) {
                    escaped = false;
                } else if (c == '\\') {
                    escaped = true;
                } else if (c == '"') {
                    inString = false;
                }
                continue;
            }
            if (c == '"') {
                inString = true;
            } else if (c == open) {
                depth++;
            } else if (c == close) {
                depth--;
                if (depth == 0) {
                    return text.substring(start, i + 1);
                }
            }
        }
        return text.substring(start);
    }
}
