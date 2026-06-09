package de.bewerbungsatze.learning;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.bewerbungsatze.jobs.Job;
import de.bewerbungsatze.jobs.JobRepository;
import de.bewerbungsatze.jobs.VectorStore;
import de.bewerbungsatze.matching.Features;
import de.bewerbungsatze.matching.FeatureScorer;
import de.bewerbungsatze.matching.JobMatch;
import de.bewerbungsatze.matching.JobMatchRepository;
import de.bewerbungsatze.matching.MatchContext;
import de.bewerbungsatze.matching.MatchStatus;
import de.bewerbungsatze.matching.UserRankingModel;
import de.bewerbungsatze.matching.UserRankingModelRepository;
import de.bewerbungsatze.matching.Weights;
import de.bewerbungsatze.profile.PreferencesRepository;
import de.bewerbungsatze.profile.ProfileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Self-Learning: trainiert pro Nutzer Score-Gewichte aus Feedback (positiv/negativ) neu
 * und verschiebt das Profil-Embedding Richtung der als passend markierten Stellen.
 */
@Service
public class SelfLearningService {

    private static final Logger log = LoggerFactory.getLogger(SelfLearningService.class);
    private static final int MIN_PER_CLASS = 3;
    private static final double DRIFT_ALPHA = 0.8;

    private static final List<MatchStatus> POSITIVE =
            List.of(MatchStatus.SAVED, MatchStatus.APPLIED, MatchStatus.INTERVIEW, MatchStatus.OFFER);
    private static final List<MatchStatus> NEGATIVE =
            List.of(MatchStatus.DISMISSED, MatchStatus.REJECTED);

    private final JobMatchRepository matchRepository;
    private final JobRepository jobRepository;
    private final ProfileRepository profileRepository;
    private final PreferencesRepository preferencesRepository;
    private final UserRankingModelRepository rankingRepository;
    private final VectorStore vectorStore;
    private final FeatureScorer featureScorer;
    private final ObjectMapper objectMapper;

    public SelfLearningService(JobMatchRepository matchRepository,
                               JobRepository jobRepository,
                               ProfileRepository profileRepository,
                               PreferencesRepository preferencesRepository,
                               UserRankingModelRepository rankingRepository,
                               VectorStore vectorStore,
                               FeatureScorer featureScorer,
                               ObjectMapper objectMapper) {
        this.matchRepository = matchRepository;
        this.jobRepository = jobRepository;
        this.profileRepository = profileRepository;
        this.preferencesRepository = preferencesRepository;
        this.rankingRepository = rankingRepository;
        this.vectorStore = vectorStore;
        this.featureScorer = featureScorer;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public RetrainResult retrain(UUID userId) {
        MatchContext ctx = MatchContext.from(
                profileRepository.findByUserId(userId).orElse(null),
                preferencesRepository.findByUserId(userId).orElse(null));

        List<JobMatch> positives = matchRepository.findByUserIdAndStatusIn(userId, POSITIVE);
        List<JobMatch> negatives = matchRepository.findByUserIdAndStatusIn(userId, NEGATIVE);

        boolean weightsTrained = false;
        double accuracy = 0;
        if (positives.size() >= MIN_PER_CLASS && negatives.size() >= MIN_PER_CLASS) {
            List<Features> samples = new ArrayList<>();
            List<Double> labels = new ArrayList<>();
            collect(userId, ctx, positives, 1.0, samples, labels);
            collect(userId, ctx, negatives, 0.0, samples, labels);

            if (samples.size() >= 2 * MIN_PER_CLASS) {
                double[] labelArray = labels.stream().mapToDouble(Double::doubleValue).toArray();
                WeightLearner.LearnResult result = WeightLearner.learn(samples, labelArray);
                saveModel(userId, result);
                weightsTrained = true;
                accuracy = result.accuracy();
            }
        }

        boolean driftApplied = applyProfileDrift(userId, positives);

        return new RetrainResult(weightsTrained, positives.size(), negatives.size(), accuracy, driftApplied);
    }

    private void collect(UUID userId, MatchContext ctx, List<JobMatch> matches, double label,
                         List<Features> samples, List<Double> labels) {
        for (JobMatch match : matches) {
            Job job = jobRepository.findById(match.getJobId()).orElse(null);
            if (job == null) {
                continue;
            }
            double semantic = vectorStore.similarity(userId, job.getId());
            samples.add(featureScorer.score(job, ctx, semantic));
            labels.add(label);
        }
    }

    private boolean applyProfileDrift(UUID userId, List<JobMatch> positives) {
        if (positives.isEmpty()) {
            return false;
        }
        List<float[]> vectors = new ArrayList<>();
        for (JobMatch match : positives) {
            float[] v = vectorStore.getJobEmbedding(match.getJobId());
            if (v.length > 0) {
                vectors.add(v);
            }
        }
        if (vectors.isEmpty()) {
            return false;
        }
        float[] profile = vectorStore.getProfileEmbedding(userId);
        float[] centroid = VectorMath.centroid(vectors);
        if (profile.length == 0 || profile.length != centroid.length) {
            return false;
        }
        float[] shifted = VectorMath.blend(profile, centroid, DRIFT_ALPHA);
        vectorStore.upsertProfileEmbedding(userId, shifted, "drift");
        return true;
    }

    private void saveModel(UUID userId, WeightLearner.LearnResult result) {
        int nextVersion = rankingRepository.findFirstByUserIdOrderByVersionDesc(userId)
                .map(m -> m.getVersion() + 1)
                .orElse(1);
        UserRankingModel model = new UserRankingModel(userId, nextVersion);
        try {
            model.setWeights(objectMapper.writeValueAsString(result.weights().asMap()));
            model.setMetrics(objectMapper.writeValueAsString(Map.of(
                    "accuracy", result.accuracy(),
                    "samples", result.samples())));
        } catch (Exception e) {
            log.warn("Modell-Serialisierung fehlgeschlagen: {}", e.getMessage());
        }
        rankingRepository.save(model);
    }

    public Weights currentWeights(UUID userId) {
        return rankingRepository.findFirstByUserIdOrderByVersionDesc(userId)
                .map(m -> {
                    try {
                        return Weights.fromMap(objectMapper.readValue(m.getWeights(), Map.class));
                    } catch (Exception e) {
                        return Weights.defaults();
                    }
                })
                .orElseGet(Weights::defaults);
    }

    public record RetrainResult(
            boolean weightsTrained,
            int positives,
            int negatives,
            double accuracy,
            boolean driftApplied) {
    }
}
