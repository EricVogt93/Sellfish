# Hexagonale Architektur (Ports & Adapters)

Das Backend ist nach **Ports & Adapters** organisiert. Jeder fachliche Kontext
(`ai`, `jobs`, `matching`, `generate`, `learning`, `docs`, `cv`, `profile`, `auth`,
`users`, `admin`, `agent`, `storage`) trennt den Anwendungskern von der Infrastruktur.

## Schichten je Kontext

```
de.bewerbungsatze.<kontext>
├── (Kern)                      Domänen-Entitäten, Value Objects, Use-Case-Services,
│                               Repository-Abstraktionen (Spring-Data-Ports)
├── port/                       Getriebene Ports (Interfaces), die der Kern nach außen definiert
└── adapter/
    ├── web/                    Inbound-Adapter: REST-Controller (treibende Seite)
    ├── out/                    Outbound-Adapter: externe API-Clients (getriebene Seite)
    ├── source/                 (jobs) Job-Quellen-Adapter, die den Port JobSource erfüllen
    └── persistence/            (jobs) JDBC-Adapter (pgvector VectorStore)
```

### Abhängigkeitsregel
Adapter → Anwendungskern → Domäne. **Der Kern kennt keine Adapter**, sondern nur die
Ports (Interfaces). Konkrete Technik (HTTP-Clients, JDBC, MinIO, Infisical) lebt ausschließlich
in `adapter/*` und wird über die Ports eingebunden (Dependency Inversion).

## Zentrale Ports

| Port (Interface)                                  | Adapter (Implementierungen)                                  |
|---------------------------------------------------|-------------------------------------------------------------|
| `ai.port.ChatProvider` / `ai.port.EmbeddingProvider` | `ai.adapter.out.{OpenAiCompatible,Ollama,Anthropic,GoogleGemini}Client` |
| `jobs.port.JobSource`                             | `jobs.adapter.source.*Source` (20 internationale Quellen)   |
| `storage.port.StorageService`                     | `storage.adapter.{FileSystem,Minio}StorageService`          |
| (Secrets)                                         | `ai.adapter.out.InfisicalClient`                            |
| (Persistence, pgvector)                           | `jobs.adapter.persistence.VectorStore`                      |

Die Spring-Data-`*Repository`-Interfaces dienen pragmatisch als Persistenz-Ports; ihre
Implementierung stellt Spring Data zur Laufzeit bereit.

## Inbound-Adapter
Alle REST-Controller liegen in `<kontext>/adapter/web` und rufen ausschließlich
Use-Case-Services des Kerns auf. Cross-Context-Orchestrierung (z. B. `agent.adapter.web.AgentController`)
nutzt die Services anderer Kontexte über deren öffentliche Schnittstellen.

## Warum so
- **Austauschbarkeit:** Ein neuer LLM-Anbieter oder eine neue Job-Quelle ist ein zusätzlicher
  Adapter hinter dem bestehenden Port – ohne Änderung am Kern.
- **Testbarkeit:** Der Kern wird mit gemockten Ports unit-getestet; Adapter werden isoliert
  gegen `MockRestServiceServer` getestet.
- **Technologie-Isolation:** Spring-/HTTP-/JDBC-Details bleiben in den Adaptern.
