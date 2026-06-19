package de.sellfish.jobs;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.sellfish.jobs.adapter.source.Fingerprints;
import de.sellfish.jobs.port.JobQuery;
import de.sellfish.jobs.port.JobSource;
import de.sellfish.jobs.port.RawJob;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class JobIngestionServiceTest {

    private final JobSourceConfigRepository sourceConfigRepository = mock(JobSourceConfigRepository.class);
    private final JobRepository jobRepository = mock(JobRepository.class);
    private final JobEmbeddingService embeddingService = mock(JobEmbeddingService.class);

    private RawJob raw(String ref, String title) {
        return new RawJob("BA", ref, title, "Acme", "Berlin", null, "desc", "url", null, null, "{}");
    }

    private JobSourceConfig enabledConfig() {
        JobSourceConfig cfg = mock(JobSourceConfig.class);
        when(cfg.getCode()).thenReturn("BA");
        when(cfg.getConfig()).thenReturn("{}");
        return cfg;
    }

    @Test
    void persistsOnlyNewJobsAndEmbedsThem() {
        JobSource source = mock(JobSource.class);
        when(source.code()).thenReturn("BA");
        RawJob fresh = raw("1", "Java Dev");
        RawJob dup = raw("2", "Python Dev");
        when(source.fetch(any(), any())).thenReturn(List.of(fresh, dup));

        JobSourceConfig cfg = enabledConfig();
        when(sourceConfigRepository.findByEnabledTrue()).thenReturn(List.of(cfg));
        when(jobRepository.existsByFingerprint(Fingerprints.of(fresh))).thenReturn(false);
        when(jobRepository.existsByFingerprint(Fingerprints.of(dup))).thenReturn(true); // bereits vorhanden
        when(jobRepository.save(any(Job.class))).thenAnswer(inv -> inv.getArgument(0));

        JobIngestionService service = new JobIngestionService(
                List.of(source), sourceConfigRepository, jobRepository, embeddingService, new ObjectMapper());

        JobIngestionService.IngestStats stats =
                service.ingest(new JobQuery(List.of("dev"), "Berlin", null, false, 10), Set.of());

        assertThat(stats.fetched()).isEqualTo(2);
        assertThat(stats.created()).isEqualTo(1);
        assertThat(stats.sources()).containsExactly("BA");
        verify(embeddingService).embedJob(any(Job.class));
    }

    @Test
    void skipsSourcesThatAreNotEnabled() {
        JobSource source = mock(JobSource.class);
        when(source.code()).thenReturn("BA");
        when(sourceConfigRepository.findByEnabledTrue()).thenReturn(List.of()); // nichts aktiv

        JobIngestionService service = new JobIngestionService(
                List.of(source), sourceConfigRepository, jobRepository, embeddingService, new ObjectMapper());

        JobIngestionService.IngestStats stats =
                service.ingest(new JobQuery(List.of("x"), null, null, false, 5), Set.of());

        assertThat(stats.fetched()).isZero();
        assertThat(stats.created()).isZero();
    }
}
