package de.sellfish.matching;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.sellfish.cv.CvStructuredRepository;
import de.sellfish.jobs.Job;
import de.sellfish.jobs.JobRepository;
import de.sellfish.jobs.MatchRecomputer;
import de.sellfish.jobs.adapter.persistence.VectorStore;
import de.sellfish.profile.PreferencesRepository;
import de.sellfish.profile.ProfileRepository;
import de.sellfish.profile.UserPreferences;
import de.sellfish.profile.UserProfile;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MatchingService implements MatchRecomputer {

    private static final Logger log = LoggerFactory.getLogger(MatchingService.class);
    private static final int SEMANTIC_CANDIDATES = 300;
    private static final int AI_VALIDATE_TOP_N = 50;

    private final ProfileRepository profileRepository;
    private final PreferencesRepository preferencesRepository;
    private final JobRepository jobRepository;
    private final JobMatchRepository matchRepository;
    private final CvStructuredRepository cvRepository;
    private final UserRankingModelRepository rankingModelRepository;
    private final VectorStore vectorStore;
    private final FeatureScorer featureScorer;
    private final JobValidationService validationService;
    private final ObjectMapper objectMapper;

    public MatchingService(
            ProfileRepository profileRepository,
            PreferencesRepository preferencesRepository,
            JobRepository jobRepository,
            JobMatchRepository matchRepository,
            CvStructuredRepository cvRepository,
            UserRankingModelRepository rankingModelRepository,
            VectorStore vectorStore,
            FeatureScorer featureScorer,
            JobValidationService validationService,
            ObjectMapper objectMapper) {
        this.profileRepository = profileRepository;
        this.preferencesRepository = preferencesRepository;
        this.jobRepository = jobRepository;
        this.matchRepository = matchRepository;
        this.cvRepository = cvRepository;
        this.rankingModelRepository = rankingModelRepository;
        this.vectorStore = vectorStore;
        this.featureScorer = featureScorer;
        this.validationService = validationService;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public int recompute(UUID userId) {
        UserProfile profile = profileRepository.findByUserId(userId).orElse(null);
        UserPreferences prefs = preferencesRepository.findByUserId(userId).orElse(null);
        String cvSkills =
                cvRepository.findByUserId(userId).map(c -> c.getSkills()).orElse("");
        MatchContext ctx = MatchContext.from(profile, prefs, cvSkills);
        Weights weights = loadWeights(userId);

        Map<UUID, Double> semantic = new HashMap<>();
        List<Job> candidates;
        if (vectorStore.hasProfileEmbedding(userId)) {
            List<VectorStore.SimilarJob> similar = vectorStore.similarJobsForUser(userId, SEMANTIC_CANDIDATES);
            similar.forEach(s -> semantic.put(s.jobId(), s.similarity()));
            candidates = jobRepository.findAllById(semantic.keySet());
        } else {
            candidates = jobRepository.findTop500ByOrderByCreatedAtDesc();
        }

        Map<UUID, JobMatch> existing =
                matchRepository.findByUserId(userId).stream().collect(Collectors.toMap(JobMatch::getJobId, m -> m));

        Map<UUID, Double> aiScores = preComputeAiScores(userId, candidates, profile, prefs);

        List<JobMatch> scored = new ArrayList<>();
        for (Job job : candidates) {
            if (featureScorer.isExcluded(job, ctx)) continue;
            double sem = semantic.getOrDefault(job.getId(), 0.0);
            double aiScore = aiScores.getOrDefault(job.getId(), 0.5);
            Features features = featureScorer.score(job, ctx, sem, aiScore);
            double total = weights.weightedScore(features);

            JobMatch match = existing.get(job.getId());
            if (match == null) match = new JobMatch(userId, job.getId());
            match.setScore(round(total));
            match.setScoreBreakdown(breakdown(features, weights, total));
            scored.add(match);
        }

        scored.sort(Comparator.comparingDouble(JobMatch::getScore).reversed());
        int rank = 1;
        for (JobMatch m : scored) m.setRank(rank++);
        matchRepository.saveAll(scored);
        return scored.size();
    }

    /** Pre-computiert AI-Relevanz-Scores für die Top-N Kandidaten. */
    private Map<UUID, Double> preComputeAiScores(
            UUID userId, List<Job> candidates, UserProfile profile, UserPreferences prefs) {
        Map<UUID, Double> map = new HashMap<>();
        int limit = Math.min(AI_VALIDATE_TOP_N, candidates.size());
        for (int i = 0; i < limit; i++) {
            Job job = candidates.get(i);
            try {
                double score = validationService.relevanceScore(userId, job, profile, prefs);
                map.put(job.getId(), score);
            } catch (Exception e) {
                map.put(job.getId(), 0.5);
            }
        }
        return map;
    }

    private Weights loadWeights(UUID userId) {
        return rankingModelRepository
                .findFirstByUserIdOrderByVersionDesc(userId)
                .map(model -> {
                    try {
                        Map<String, Object> map =
                                objectMapper.readValue(model.getWeights(), new TypeReference<Map<String, Object>>() {});
                        return map.isEmpty() ? Weights.defaults() : Weights.fromMap(map);
                    } catch (Exception e) {
                        return Weights.defaults();
                    }
                })
                .orElseGet(Weights::defaults);
    }

    private String breakdown(Features features, Weights weights, double total) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("features", features.asMap());
        map.put("weights", weights.asMap());
        map.put("total", round(total));
        try {
            return objectMapper.writeValueAsString(map);
        } catch (Exception e) {
            return "{}";
        }
    }

    private double round(double v) {
        return Math.round(v * 10000.0) / 10000.0;
    }
}
