package de.sellfish.docs.adapter.web;

import de.sellfish.docs.SmartImportService;
import de.sellfish.docs.SmartImportService.SmartImportResult;
import de.sellfish.profile.ProfileService;
import de.sellfish.profile.UserPreferences;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/documents")
public class SmartImportController {

    private final SmartImportService smartImportService;
    private final ProfileService profileService;

    public SmartImportController(SmartImportService smartImportService, ProfileService profileService) {
        this.smartImportService = smartImportService;
        this.profileService = profileService;
    }

    @PostMapping("/smart-import")
    public ResponseEntity<SmartImportResult> smartImport(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "applyPreferences", defaultValue = "true") boolean applyPrefs) {
        UUID userId = de.sellfish.common.security.CurrentUser.id();

        SmartImportResult result = smartImportService.importSmart(userId, file);

        // Auto-apply derived preferences (merge with existing, don't overwrite)
        if (applyPrefs && !result.suggestions().titles().isEmpty()) {
            UserPreferences prefs = profileService.getOrCreatePreferences(userId);
            var existingTitles = new java.util.HashSet<>(java.util.Arrays.asList(prefs.getDesiredTitles()));
            var existingKeywords = new java.util.HashSet<>(java.util.Arrays.asList(prefs.getKeywords()));
            var existingExcluded = new java.util.HashSet<>(java.util.Arrays.asList(prefs.getExcludedCompanies()));

            existingTitles.addAll(result.suggestions().titles());
            existingKeywords.addAll(result.suggestions().keywords());
            existingExcluded.addAll(result.suggestions().excludedCompanies());

            prefs.setDesiredTitles(existingTitles.toArray(new String[0]));
            prefs.setKeywords(existingKeywords.toArray(new String[0]));
            prefs.setExcludedCompanies(existingExcluded.toArray(new String[0]));
            profileService.save(prefs);
        }

        return ResponseEntity.ok(result);
    }
}
