package de.bewerbungsatze.matching;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import de.bewerbungsatze.jobs.Job;
import de.bewerbungsatze.jobs.JobRepository;
import de.bewerbungsatze.jobs.MatchRecomputer;
import de.bewerbungsatze.jobs.VectorStore;
import de.bewerbungsatze.profile.PreferencesRepository;
import de.bewerbungsatze.profile.ProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Berechnet Job-Matches: Hard-Filter → semantische Ähnlichkeit → gewichteter Feature-Score.
 */
@Service
public class MatchingService implements MatchRecomputer {

    private static final int SEMANTIC_CANDIDATES = 300;

    private final ProfileRepository profileRepository;
    private final PreferencesRepository preferencesRepository;
    private final JobRepository jobRepository;
    private final JobMatchRepository matchRepository;
    private final UserRankingModelRepository rankingModelRepository;
    private final VectorStore vectorStore;
    private final FeatureScorer featureScorer;
    private final ObjectMapper objectMapper;

    public MatchingService(ProfileRepository profileRepository,
                           PreferencesRepository preferencesRepository,
                           JobRepository jobRepository,
                           JobMatchRepository matchRepository,
                           UserRankingModelRepository rankingModelRepository,
                           VectorStore vectorStore,
                           FeatureScorer featureScorer,
                           ObjectMapper objectMapper) {
        this.profileRepository = profileRepository;
        this.preferencesRepository = preferencesRepository;
        this.jobRepository = jobRepository;
        this.matchRepository = matchRepository;
        this.rankingModelRepository = rankingModelRepository;
        this.vectorStore = vectorStore;
        this.featureScorer = featureScorer;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public int recompute(UUID userId) {
        MatchContext ctx = MatchContext.from(
                profileRepository.findByUserId(userId).orElse(null),
                preferencesRepository.findByUserId(userId).orElse(null));
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

        Map<UUID, JobMatch> existing = matchRepository.findByUserId(userId).stream()
                .collect(Collectors.toMap(JobMatch::getJobId, m -> m));

        List<JobMatch> scored = new ArrayList<>();
        for (Job job : candidates) {
            if (featureScorer.isExcluded(job, ctx)) {
                continue;
            }
            double sem = semantic.getOrDefault(job.getId(), 0.0);
            Features features = featureScorer.score(job, ctx, sem);
            double total = weights.weightedScore(features);

            JobMatch match = existing.get(job.getId());
            if (match == null) {
                match = new JobMatch(userId, job.getId());
            }
            match.setScore(round(total));
            match.setScoreBreakdown(breakdown(features, weights, total));
            scored.add(match);
        }

        scored.sort(Comparator.comparingDouble(JobMatch::getScore).reversed());
        int rank = 1;
        for (JobMatch m : scored) {
            m.setRank(rank++);
        }
        matchRepository.saveAll(scored);
        return scored.size();
    }

    private Weights loadWeights(UUID userId) {
        return rankingModelRepository.findFirstByUserIdOrderByVersionDesc(userId)
                .map(model -> {
                    try {
                        Map<String, Object> map = objectMapper.readValue(
                                model.getWeights(), new TypeReference<Map<String, Object>>() {
                                });
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
