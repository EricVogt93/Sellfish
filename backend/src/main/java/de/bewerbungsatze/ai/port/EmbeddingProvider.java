package de.bewerbungsatze.ai.port;

import de.bewerbungsatze.ai.Provider;
import de.bewerbungsatze.ai.model.ResolvedModel;

public interface EmbeddingProvider {

    boolean supports(Provider provider);

    float[] embed(ResolvedModel model, String text);
}
