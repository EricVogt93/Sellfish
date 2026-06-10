# LLM-Provider einrichten (M2)

Provider werden in der Tabelle `llm_provider_config` gepflegt – global (`user_id = NULL`, durch Admin)
oder pro Nutzer. Jeder Eintrag hat einen `purpose` (`CHAT` oder `EMBEDDING`).

## Felder
- `provider`: `OLLAMA` | `OPENAI` | `ANTHROPIC` | `GOOGLE` | `NIM` | `OPENAI_COMPATIBLE`
- `model`: z. B. `llama3.1`, `gpt-4o`, `claude-sonnet-4-6`, `nomic-embed-text`
- `base_url`: bei self-hosted/kompatiblen Endpunkten setzen (z. B. `http://ollama:11434`)
- `key_ref`: Infisical-Secret-Pfad für zentrale Keys **oder**
- `key_enc`: AES-GCM-verschlüsselter Per-User-Key (vom Backend gesetzt, nie im Klartext)

## Beispiele

**Self-hosted Ollama (kostenlos, kein Key):**
```
provider=OLLAMA, model=llama3.1, base_url=http://ollama:11434, purpose=CHAT
provider=OLLAMA, model=nomic-embed-text, base_url=http://ollama:11434, purpose=EMBEDDING
```

**OpenAI / ChatGPT:**
```
provider=OPENAI, model=gpt-4o, key_ref=/llm/openai-key, purpose=CHAT
```

**NVIDIA NIM (OpenAI-kompatibel):**
```
provider=OPENAI_COMPATIBLE, model=meta/llama-3.1-70b-instruct,
base_url=https://integrate.api.nvidia.com/v1, key_ref=/llm/nim-key, purpose=CHAT
```

**Anthropic / Claude:**
```
provider=ANTHROPIC, model=claude-sonnet-4-6, key_ref=/llm/anthropic-key, purpose=CHAT
```

> **Embedding-Dimension:** Über `EMBEDDING_DIM` (Default 768, passend zu `nomic-embed-text`)
> konfigurierbar — z. B. `1536` für OpenAI `text-embedding-3-small`. Der Wert legt beim **ersten**
> DB-Start die pgvector-Spalten an und kann danach nicht mehr per ENV geändert werden (dann
> Folge-Migration nötig). Liefert ein Modell eine abweichende Dimension, werden dessen Embeddings
> mit einer Warnung verworfen statt still zu scheitern.
