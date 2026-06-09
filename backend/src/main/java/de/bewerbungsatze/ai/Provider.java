package de.bewerbungsatze.ai;

/**
 * Unterstützte LLM-Anbieter. {@code OPENAI_COMPATIBLE} und {@code NIM} decken alle
 * OpenAI-kompatiblen Endpunkte ab (ChatGPT-Proxy, Kimi, OpenRouter, NVIDIA NIM, …).
 */
public enum Provider {
    OLLAMA,
    OPENAI,
    NIM,
    OPENAI_COMPATIBLE,
    ANTHROPIC,
    GOOGLE
}
