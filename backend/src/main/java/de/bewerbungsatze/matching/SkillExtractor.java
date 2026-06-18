package de.bewerbungsatze.matching;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extrahiert Skills aus Fließtext (Job-Beschreibungen, CVs) via Regex-Patterns
 * und gleicht sie ab. Leichtgewichtig, keine LLM-Abhängigkeit.
 */
public final class SkillExtractor {

    private static final Pattern SKILL_PATTERN = Pattern.compile(
            "\\b(?:Java|Python|JavaScript|TypeScript|Go|Rust|C\\+\\+|C#|Ruby|PHP|Scala|Kotlin|Swift|Dart|"
                    + "React|Angular|Vue|Svelte|Next\\.?js|Node\\.?js|Express|Spring|Django|Flask|FastAPI|Rails|Laravel|"
                    + "Kubernetes|Docker|Terraform|Ansible|Jenkins|GitLab CI|GitHub Actions|AWS|Azure|GCP|"
                    + "SQL|PostgreSQL|MySQL|MongoDB|Redis|Kafka|RabbitMQ|Elasticsearch|GraphQL|REST|gRPC|"
                    + "Linux|Bash|PowerShell|Git|CI/CD|DevOps|MLOps|Machine Learning|Deep Learning|NLP|LLM|"
                    + "Agile|Scrum|Kanban|Jira|Confluence|Figma|"
                    + "Prometheus|Grafana|Datadog|Sentry|ELK|Nginx|Apache|"
                    + "Android|iOS|Flutter|React Native|Electron|Tailwind|SASS|CSS|HTML|"
                    + "Spark|Hadoop|Airflow|Snowflake|Databricks|dbt|Looker|Tableau|Power BI|"
                    + "Selenium|Cypress|JUnit|PyTest|Jest|Mocha|Playwright|Testcontainers)\\\\b",
            Pattern.CASE_INSENSITIVE);

    private SkillExtractor() {}

    public static Set<String> extract(String text) {
        if (text == null || text.isBlank()) return Set.of();
        Set<String> skills = new LinkedHashSet<>();
        Matcher m = SKILL_PATTERN.matcher(text);
        while (m.find()) {
            skills.add(m.group().toLowerCase(Locale.ROOT));
        }
        return skills;
    }

    /**
     * Skill-Überlappung: Anteil der Job-Skills, die auch in den Profil-Skills vorkommen.
     */
    public static double overlap(String jobText, String profileText) {
        Set<String> jobSkills = extract(jobText);
        Set<String> profileSkills = extract(profileText);
        if (jobSkills.isEmpty() || profileSkills.isEmpty()) return 0.0;
        Set<String> intersection = new LinkedHashSet<>(jobSkills);
        intersection.retainAll(profileSkills);
        return (double) intersection.size() / jobSkills.size();
    }
}
