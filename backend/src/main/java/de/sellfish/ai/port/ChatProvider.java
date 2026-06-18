package de.sellfish.ai.port;

import de.sellfish.ai.Provider;
import de.sellfish.ai.model.ChatRequest;
import de.sellfish.ai.model.ChatResult;
import de.sellfish.ai.model.ResolvedModel;

public interface ChatProvider {

    boolean supports(Provider provider);

    ChatResult chat(ResolvedModel model, ChatRequest request);
}
