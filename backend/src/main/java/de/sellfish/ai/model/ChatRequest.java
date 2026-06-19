package de.sellfish.ai.model;

import java.util.List;

public record ChatRequest(List<ChatMessage> messages, Double temperature, Integer maxTokens) {

    public static ChatRequest of(String system, String user) {
        return new ChatRequest(List.of(ChatMessage.system(system), ChatMessage.user(user)), 0.4, null);
    }
}
