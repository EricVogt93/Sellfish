package de.sellfish.ai.model;

public record ChatResult(String content, String model, Integer promptTokens, Integer completionTokens) {}
