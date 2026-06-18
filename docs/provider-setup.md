# LLM Provider Setup

Providers are managed in the `llm_provider_config` table — either globally (`user_id = NULL`,
set by an admin) or per user. Each entry has a `purpose` (`CHAT` or `EMBEDDING`).

## Fields
- `provider`: `OLLAMA` | `OPENAI` | `ANTHROPIC` | `GOOGLE` | `NIM` | `OPENAI_COMPATIBLE`
- `model`: e.g. `llama3.1`, `gpt-4o`, `claude-sonnet-4-6`, `nomic-embed-text`
- `base_url`: set for self-hosted or compatible endpoints (e.g. `http://ollama:11434`)
- `key_ref`: Infisical secret path for central keys **or**
- `key_enc`: AES-GCM-encrypted per-user key (set by the backend, never stored in plaintext)

## Examples

**Self-hosted Ollama (free, no key):**
```
provider=OLLAMA, model=llama3.1, base_url=http://ollama:11434, purpose=CHAT
provider=OLLAMA, model=nomic-embed-text, base_url=http://ollama:11434, purpose=EMBEDDING
```

**OpenAI / ChatGPT:**
```
provider=OPENAI, model=gpt-4o, key_ref=/llm/openai-key, purpose=CHAT
```

**NVIDIA NIM (OpenAI-compatible):**
```
provider=OPENAI_COMPATIBLE, model=meta/llama-3.1-70b-instruct,
base_url=https://integrate.api.nvidia.com/v1, key_ref=/llm/nim-key, purpose=CHAT
```

**Anthropic / Claude:**
```
provider=ANTHROPIC, model=claude-sonnet-4-6, key_ref=/llm/anthropic-key, purpose=CHAT
```

> **Embedding dimension:** Configurable via `EMBEDDING_DIM` (default 768, matching
> `nomic-embed-text`) — e.g. `1536` for OpenAI `text-embedding-3-small`. This value creates
> the pgvector columns on the **first** DB start and cannot be changed via env afterward
> (requires a follow-up migration). If a model produces a different dimension, its embeddings
> are discarded with a warning instead of failing silently.
