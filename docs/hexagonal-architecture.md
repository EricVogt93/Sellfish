# Hexagonal Architecture (Ports & Adapters)

The backend is organized following **Ports & Adapters**. Each business context
(`ai`, `jobs`, `matching`, `generate`, `learning`, `docs`, `cv`, `profile`, `auth`,
`users`, `admin`, `agent`, `storage`) separates the application core from infrastructure.

## Layers per Context

```
de.bewerbungsatze.<context>
├── (core)                      Domain entities, value objects, use-case services,
│                               repository abstractions (Spring Data ports)
├── port/                       Driven ports (interfaces) the core defines for the outside
└── adapter/
    ├── web/                    Inbound adapter: REST controllers (driving side)
    ├── out/                    Outbound adapter: external API clients (driven side)
    ├── source/                 (jobs) Job source adapters implementing the JobSource port
    └── persistence/            (jobs) JDBC adapter (pgvector VectorStore)
```

### Dependency Rule
Adapter → Application Core → Domain. **The core knows no adapters**, only the
ports (interfaces). Concrete technology (HTTP clients, JDBC, MinIO, Infisical) lives
exclusively in `adapter/*` and is wired in through the ports (dependency inversion).

## Central Ports

| Port (Interface) | Adapters (Implementations) |
|------------------|---------------------------|
| `ai.port.ChatProvider` / `ai.port.EmbeddingProvider` | `ai.adapter.out.{OpenAiCompatible,Ollama,Anthropic,GoogleGemini}Client` |
| `jobs.port.JobSource` | `jobs.adapter.source.*Source` (33 international sources) |
| `storage.port.StorageService` | `storage.adapter.{FileSystem,Minio}StorageService` |
| (Secrets) | `ai.adapter.out.InfisicalClient` |
| (Persistence, pgvector) | `jobs.adapter.persistence.VectorStore` |

The Spring Data `*Repository` interfaces serve pragmatically as persistence ports; their
implementation is provided by Spring Data at runtime.

## Inbound Adapters
All REST controllers live in `<context>/adapter/web` and call only use-case services from
the core. Cross-context orchestration (e.g., `agent.adapter.web.AgentController`) uses the
public interfaces of other contexts' services.

## Why This Way
- **Replaceability:** A new LLM provider or job source is just another adapter behind an
  existing port — no core changes needed.
- **Testability:** The core is unit-tested with mocked ports; adapters are tested in isolation
  against `MockRestServiceServer`.
- **Technology isolation:** Spring/HTTP/JDBC details stay in the adapters.
