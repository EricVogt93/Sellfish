# Architektur

Bewerbungsatze ist ein **modularer Monolith** (ein deploybares Spring-Boot-Backend) plus
SvelteKit-Frontend und PostgreSQL. Hintergrundarbeit (Job-Suche, Embeddings, Self-Learning-Retraining)
läuft asynchron über Scheduler/Async-Executor.

## Module (Backend, `de.bewerbungsatze.*`)

| Paket        | Verantwortung |
|--------------|---------------|
| `auth`       | JWT-Erzeugung/-Validierung, Login/Registrierung, UserDetails |
| `users`      | Nutzer-Entität, `/api/me`, Admin-Verwaltung |
| `profile`    | Profil, Wünsche, Filter (harte Filter vs. weiche Gewichte) |
| `docs`       | Upload & Parsing von CV/Zertifikaten/Zeugnissen/Anschreiben/Projektlisten *(M2)* |
| `ai`         | LLM-Provider-Abstraktion (Spring AI), Provider-Auswahl pro Nutzer, Embeddings *(M2)* |
| `jobs`       | Job-Quellen (Bundesagentur, Adzuna, Scraper, LLM-Websuche), Ingestion, Dedupe *(M3)* |
| `matching`   | Hard-Filter → Semantik (pgvector) → Feature-Score → optional LLM-Rerank *(M4)* |
| `generate`   | Generatoren: Tailored-CV, Anschreiben, Motivation, Bewerbungstext *(M5)* |
| `learning`   | Feedback-Erfassung, Profil-Drift, Gewichts-Retraining *(M6)* |
| `agent`      | Externe-Agenten-API / MCP-Schnittstelle *(M7)* |
| `storage`    | Datei-Storage-Abstraktion (MinIO / FS) *(M2)* |
| `common`     | Basis-Entität, Security-Config, Fehlerbehandlung, Properties |

## Datenmodell

Vollständig in `backend/src/main/resources/db/migration/V1__init.sql`. Kerntabellen: `users`,
`user_profile`, `user_preferences`, `documents`, `cv_structured`, `projects`, `profile_embedding`,
`job_sources`, `jobs`, `job_embedding`, `job_matches`, `feedback_events`, `generated_documents`,
`llm_provider_config`, `search_runs`, `user_ranking_model`.

Embeddings werden als `vector(768)` (pgvector) gespeichert; die Job-Embeddings haben einen
HNSW-Cosine-Index für schnelle Ähnlichkeitssuche.

## LLM-Provider-Abstraktion (M2)

Eigene, dependency-leichte Schicht über Spring `RestClient` (kein Spring AI). Ein `ProviderResolver`
wählt pro Nutzer und Zweck (`CHAT`/`EMBEDDING`) die konkrete Konfiguration aus `llm_provider_config`,
löst den Key auf (Infisical bzw. AES-entschlüsselt) und liefert ein `ResolvedModel`. Der `LlmService`
dispatcht an den passenden Client (`ChatProvider`/`EmbeddingProvider`):

- **self-hosted:** `OllamaClient` (`base_url` auf eigene Instanz)
- **OpenAI/ChatGPT, NVIDIA NIM, Kimi, OpenRouter, …:** `OpenAiCompatibleClient` mit konfigurierbarer Base-URL
- **Anthropic/Claude:** `AnthropicClient`
- **Google/Gemini:** `GoogleGeminiClient`

Jeder Client ist isoliert per `MockRestServiceServer` unit-getestet.

Globale Keys kommen aus **Infisical** (Machine-Identity), Per-User-Keys werden AES-GCM-verschlüsselt
in der DB abgelegt. Siehe [`provider-setup.md`](provider-setup.md) und [`infisical-setup.md`](infisical-setup.md).

## Matching & Self-Learning

1. **Hard-Filter** (SQL): Standort/Gehalt/Vertragsart/Ausschlüsse.
2. **Semantik** (pgvector): Cosine-Ähnlichkeit Profil↔Job.
3. **Feature-Score**: gewichtete Merkmale; Gewichte aus `user_ranking_model`.
4. **LLM-Rerank** (optional): Top-N mit Begründung, Few-Shot aus Nutzer-Feedback.

Das Self-Learning sammelt `feedback_events`, verschiebt das Profil-Embedding Richtung positiver
Treffer (Centroid-Shift) und trainiert periodisch ein leichtes, erklärbares Modell (z. B. logistische
Regression) pro Nutzer neu.

## Sicherheit

- Stateless JWT (Access/Refresh), BCrypt-Passwörter.
- Strikte user-Scoping aller Daten.
- Secrets nie im Git: zentrale Keys via Infisical, Per-User-Keys AES-GCM-verschlüsselt.
- DSGVO: Export/Löschung pro Nutzer; Audit über `search_runs` und `feedback_events`.
