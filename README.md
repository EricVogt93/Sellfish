# Bewerbungsatze

AI-gestützte, selbst-hostbare Plattform für Job-Suche und Bewerbungs-Assistenz.

Sie findet pro Nutzer passende Stellen (basierend auf Lebenslauf, Wünschen, Filtern und gelernten
Signalen) und hilft beim Bewerben: stellenangepasste Lebensläufe, Anschreiben, Motivationsschreiben
und Bewerbungstexte. Die KI-Anbindung ist provider-agnostisch (self-hosted Ollama, OpenAI/ChatGPT,
Anthropic/Claude, Google, NVIDIA NIM, …). Über die Zeit verbessert ein Self-Learning-Prozess die
Treffer.

## Tech-Stack

- **Backend:** Java 21, Spring Boot 3.4, PostgreSQL + pgvector, Flyway, Spring Security (JWT)
- **Frontend:** SvelteKit (TypeScript), Node-Adapter
- **KI:** Spring AI (Multiprovider) – wird in M2 angebunden
- **Secrets:** Infisical (zentrale Keys) + AES-GCM-verschlüsselte Per-User-Keys
- **Storage:** MinIO (S3-kompatibel) oder lokales Dateisystem

## Projektstruktur

```
backend/    Spring-Boot-Anwendung (modularer Monolith)
frontend/   SvelteKit-Web-App
infra/      docker-compose, .env.example
docs/       Architektur- und Setup-Doku
```

## Lokal starten

### Voraussetzungen
- JDK 21, Maven 3.9+
- Node 22+
- Docker (für Postgres + MinIO via docker-compose)

### 1. Infrastruktur (Postgres + MinIO)
```bash
cp infra/.env.example infra/.env   # Werte anpassen
docker compose -f infra/docker-compose.yml up -d postgres minio
```

### 2. Backend
```bash
cd backend
mvn spring-boot:run
# Swagger UI: http://localhost:8080/swagger-ui.html
# Health:     http://localhost:8080/actuator/health
```

### 3. Frontend
```bash
cd frontend
npm install
npm run dev   # http://localhost:5173 (proxyt /api an :8080)
```

### Alles per Docker
```bash
docker compose -f infra/docker-compose.yml up --build
```

## Tests

- **Backend:** `cd backend && mvn verify` – nutzt Testcontainers (benötigt Docker) für einen
  echten Postgres mit pgvector und prüft Auth-/Profil-Flow End-to-End.
- **Frontend:** `cd frontend && npm run check`

## Umsetzungs-Roadmap

| Meilenstein | Inhalt | Status |
|-------------|--------|--------|
| **M0** | Gerüst: Repo-Struktur, docker-compose, Spring-Boot- & SvelteKit-Skeleton, Flyway-Schema, CI | ✅ |
| **M1** | Auth & Multi-User (JWT), Profil + Wünsche/Filter | ✅ |
| **M2** | Dokumenten-Upload + Parsing, LLM-Provider-Abstraktion (Spring AI), Infisical | ⏳ |
| **M3** | Job-Ingestion (Bundesagentur-API, Adzuna), Embeddings | ⏳ |
| **M4** | Matching & Ranking, Dashboard mit Score-Begründung | ⏳ |
| **M5** | Generatoren: Tailored-CV, Anschreiben, Motivation, Bewerbungstext | ⏳ |
| **M6** | Self-Learning: Feedback, Profil-Drift, Gewichts-Retraining | ⏳ |
| **M7** | Scraper, LLM-Websuche, externe-Agenten-API (MCP), Admin | ⏳ |

Details siehe [`docs/architecture.md`](docs/architecture.md).
