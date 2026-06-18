package de.sellfish.ai.port;

import de.sellfish.ai.Provider;
import de.sellfish.ai.model.ResolvedModel;

public interface EmbeddingProvider {

    boolean supports(Provider provider);

    float[] embed(ResolvedModel model, String text);
}
