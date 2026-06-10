package de.bewerbungsatze.ai.port;

import de.bewerbungsatze.ai.Provider;
import de.bewerbungsatze.ai.model.ChatRequest;
import de.bewerbungsatze.ai.model.ChatResult;
import de.bewerbungsatze.ai.model.ResolvedModel;

public interface ChatProvider {

    boolean supports(Provider provider);

    ChatResult chat(ResolvedModel model, ChatRequest request);
}
