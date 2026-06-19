package de.sellfish.matching;

import de.sellfish.common.text.TextTokens;
import de.sellfish.jobs.Job;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

@Component
public class FeatureScorer {

    private static final double RECENCY_HALF_LIFE_DAYS = 30.0;

    private final JobValidationService validationService;

    public FeatureScorer(JobValidationService validationService) {
        this.validationService = validationService;
    }

    /** Alle Features inkl. Skill-Overlap. AI-Relevanz separat via AI-Service. */
    public Features score(Job job, MatchContext ctx, double semantic, double aiRelevance) {
        String jobText = jobTextForSkills(job);
        return new Features(
                clamp(semantic),
                titleScore(job, ctx),
                keywordScore(job, ctx),
                locationScore(job, ctx),
                recencyScore(job),
                remoteScore(job, ctx),
                SkillExtractor.overlap(jobText, ctx.profileText()),
                aiRelevance);
    }

    /** Score without AI (zur Verwendung im Self-Learning, das keine LLM-Calls macht). */
    public Features score(Job job, MatchContext ctx, double semantic) {
        return score(job, ctx, semantic, 0.5);
    }

    public boolean isExcluded(Job job, MatchContext ctx) {
        if (job.getCompany() == null || ctx.excludedCompanies().isEmpty()) return false;
        String company = job.getCompany().toLowerCase(Locale.ROOT);
        return ctx.excludedCompanies().stream().anyMatch(company::contains);
    }

    private double titleScore(Job job, MatchContext ctx) {
        if (ctx.desiredTitles().isEmpty()) return 0.0;
        Set<String> jobTokens = TextTokens.tokenize(job.getTitle());
        if (jobTokens.isEmpty()) return 0.0;
        int matched = 0;
        for (String title : ctx.desiredTitles()) {
            if (!TextTokens.tokenize(title).isEmpty() && overlaps(TextTokens.tokenize(title), jobTokens)) matched++;
        }
        return (double) matched / ctx.desiredTitles().size();
    }

    private double keywordScore(Job job, MatchContext ctx) {
        if (ctx.keywords().isEmpty()) return 0.0;
        String haystack = (job.getTitle() + " " + nz(job.getDescription())).toLowerCase(Locale.ROOT);
        long matched = ctx.keywords().stream().map(k -> k.toLowerCase(Locale.ROOT)).filter(haystack::contains).count();
        return (double) matched / ctx.keywords().size();
    }

    private double locationScore(Job job, MatchContext ctx) {
        if (ctx.location() == null || ctx.location().isBlank()) return 0.0;
        if ("REMOTE".equalsIgnoreCase(ctx.remotePref())) return 1.0;
        return TextTokens.containsAny(job.getLocation(), TextTokens.tokenize(ctx.location())) ? 1.0 : 0.0;
    }

    private double recencyScore(Job job) {
        if (job.getPostedAt() == null) return 0.5;
        double days = Math.max(0, Duration.between(job.getPostedAt(), Instant.now()).toHours() / 24.0);
        return Math.pow(0.5, days / RECENCY_HALF_LIFE_DAYS);
    }

    private double remoteScore(Job job, MatchContext ctx) {
        String pref = ctx.remotePref() == null ? "ANY" : ctx.remotePref().toUpperCase(Locale.ROOT);
        boolean jobRemote = TextTokens.containsAny(
                nz(job.getRemote()) + " " + nz(job.getDescription()),
                Set.of("remote", "homeoffice", "home office", "telearbeit"));
        return switch (pref) {
            case "REMOTE" -> jobRemote ? 1.0 : 0.2;
            case "HYBRID" -> jobRemote ? 0.8 : 0.5;
            case "ONSITE" -> jobRemote ? 0.4 : 1.0;
            default -> 0.5;
        };
    }

    private String jobTextForSkills(Job job) {
        return (nz(job.getTitle()) + " " + nz(job.getDescription()));
    }

    private boolean overlaps(Set<String> a, Set<String> b) {
        Set<String> copy = new HashSet<>(a);
        copy.retainAll(b);
        return !copy.isEmpty();
    }

    private String nz(String s) { return s == null ? "" : s; }
    private double clamp(double v) { return Math.max(0.0, Math.min(1.0, v)); }
}
