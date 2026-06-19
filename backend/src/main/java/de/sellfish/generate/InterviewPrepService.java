package de.sellfish.generate;

import de.sellfish.ai.LlmService;
import de.sellfish.ai.model.ChatRequest;
import de.sellfish.ai.model.ChatResult;
import de.sellfish.common.error.ApiException;
import de.sellfish.jobs.Job;
import de.sellfish.jobs.JobRepository;
import de.sellfish.matching.JobMatch;
import de.sellfish.matching.JobMatchRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class InterviewPrepService {

    private final LlmService llmService;
    private final JobMatchRepository matchRepository;
    private final JobRepository jobRepository;

    public InterviewPrepService(
            LlmService llmService, JobMatchRepository matchRepository, JobRepository jobRepository) {
        this.llmService = llmService;
        this.matchRepository = matchRepository;
        this.jobRepository = jobRepository;
    }

    public String generateQuestions(UUID userId, UUID matchId) {
        JobMatch match = matchRepository
                .findById(matchId)
                .filter(m -> m.getUserId().equals(userId))
                .orElseThrow(() -> ApiException.notFound("Match not found"));
        Job job = jobRepository.findById(match.getJobId()).orElseThrow(() -> ApiException.notFound("Job not found"));

        String prompt =
                """
                You are an expert technical interviewer. Generate 8 specific interview questions
                for this job. Mix technical, behavioral, and role-specific questions.
                Format as a numbered list. Keep each question concise (1-2 sentences).

                Job Title: %s
                Company: %s
                Description: %s

                Interview Questions:
                """
                        .formatted(job.getTitle(), nz(job.getCompany()), truncate(nz(job.getDescription()), 2000));

        ChatResult result = llmService.chat(
                userId,
                ChatRequest.of(
                        "You are an expert technical interviewer. Answer in the requested format only.", prompt));
        return result.content() != null ? result.content() : "Could not generate questions.";
    }

    public String generateCompanyResearch(UUID userId, UUID matchId) {
        JobMatch match = matchRepository
                .findById(matchId)
                .filter(m -> m.getUserId().equals(userId))
                .orElseThrow(() -> ApiException.notFound("Match not found"));
        Job job = jobRepository.findById(match.getJobId()).orElseThrow(() -> ApiException.notFound("Job not found"));

        if (job.getCompany() == null || job.getCompany().isBlank()) {
            return "No company name available for research.";
        }

        String prompt =
                """
                You are a company research analyst. Provide a concise company profile for %s.
                Include: what they do, industry, size estimate, culture hints from the job description,
                and 2-3 tips for interviewing there. Keep it to 4-5 short paragraphs.
                Be honest about what you can infer vs what you know for certain.

                Job Description: %s

                Company Profile for %s:
                """
                        .formatted(job.getCompany(), truncate(nz(job.getDescription()), 1500), job.getCompany());

        ChatResult result = llmService.chat(
                userId,
                ChatRequest.of(
                        "You are a company research analyst. Be concise and honest. Acknowledge uncertainty.", prompt));
        return result.content() != null ? result.content() : "Could not research company.";
    }

    private String nz(String s) {
        return s == null ? "" : s;
    }

    private String truncate(String s, int max) {
        return s.length() > max ? s.substring(0, max) + "..." : s;
    }
}
