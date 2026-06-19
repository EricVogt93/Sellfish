package de.sellfish.account;

import de.sellfish.ai.LlmProviderConfigRepository;
import de.sellfish.cv.CvStructuredRepository;
import de.sellfish.cv.ProjectRepository;
import de.sellfish.docs.DocumentRepository;
import de.sellfish.feedback.FeedbackEventRepository;
import de.sellfish.generate.GeneratedDocumentRepository;
import de.sellfish.matching.JobMatchRepository;
import de.sellfish.profile.PreferencesRepository;
import de.sellfish.profile.ProfileRepository;
import org.springframework.stereotype.Component;

/**
 * Facade for the 9 user-data repositories needed by AccountService.
 * Groups them by aggregate root so AccountService has 3 dependencies
 * instead of 11.
 */
@Component
public record UserDataReaders(
        ProfileRepository profiles,
        PreferencesRepository preferences,
        CvStructuredRepository cv,
        ProjectRepository projects,
        DocumentRepository documents,
        JobMatchRepository matches,
        GeneratedDocumentRepository generatedDocs,
        FeedbackEventRepository feedback,
        LlmProviderConfigRepository llmConfigs) {}
