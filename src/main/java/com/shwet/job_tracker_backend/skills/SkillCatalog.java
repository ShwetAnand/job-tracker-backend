package com.shwet.job_tracker_backend.skills;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Dictionary of technical skills with aliases, used for deterministic
 * skill detection in job descriptions / resume text and for normalising skill
 * names returned by the LLM.
 *
 * Matching is done on whole tokens. The previous implementation used
 * String.contains(), which made "c" match every text containing the letter c,
 * "java" match "javascript", "rest" match "interest", and so on.
 *
 * Limitation: only skills present in this catalog can be detected deterministically.
 */
public final class SkillCatalog {

    private record Skill(String canonical, List<String> aliases) {
    }

    private static Skill skill(String canonical, String... aliases) {
        return new Skill(canonical, List.of(aliases));
    }

    /** Matched case-insensitively. Spaces in an alias also match hyphens or multiple spaces. */
    private static final List<Skill> SKILLS = List.of(
            skill("java"),
            skill("c++"),
            skill("c#"),
            skill("python"),
            skill("javascript", "js", "es6", "ecmascript"),
            skill("typescript"),
            skill("kotlin"),
            skill("php"),
            skill("sql"),
            skill("html", "html5"),
            skill("css", "css3"),

            skill("spring", "spring boot", "springboot", "spring framework", "spring mvc",
                    "spring data", "spring data jpa"),
            skill("spring security"),
            skill("spring ai"),
            skill("react", "react.js", "reactjs"),
            skill("angular", "angularjs"),
            skill("vue", "vue.js", "vuejs"),
            skill("node.js", "nodejs", "node"),
            skill("express.js", "expressjs"),

            skill("mysql"),
            skill("postgresql", "postgres"),
            skill("mongodb", "mongo"),
            skill("redis"),

            skill("rest", "rest api", "rest apis", "restful", "restful api", "restful apis"),
            skill("graphql"),
            skill("microservices", "microservice"),
            skill("jwt", "json web token", "json web tokens"),
            skill("oauth", "oauth2", "oauth 2.0", "oauth2.0"),
            skill("hibernate"),
            skill("jpa", "java persistence api"),

            skill("git"),
            skill("github"),
            skill("gitlab"),
            skill("docker"),
            skill("kubernetes", "k8s"),
            skill("maven"),
            skill("gradle"),
            skill("jenkins"),
            skill("ci/cd", "cicd", "ci cd"),
            skill("junit"),
            skill("mockito"),
            skill("linux"),
            skill("kafka"),
            skill("rabbitmq"),

            skill("aws", "amazon web services"),
            skill("azure"),
            skill("gcp", "google cloud", "google cloud platform"),

            skill("rag", "retrieval augmented generation"),
            skill("llm", "llms", "large language model", "large language models"),
            skill("machine learning"),
            skill("artificial intelligence"),
            skill("nlp", "natural language processing"),

            skill("data structures", "data structure", "dsa"),
            skill("algorithms", "algorithm"),
            skill("oop", "oops", "object oriented programming", "object oriented"),
            skill("dbms"),
            skill("operating systems", "operating system"),
            skill("computer networks", "computer network", "computer networking"),
            skill("system design"),
            skill("agile")
    );

    /**
     * Short or ambiguous tokens that are only accepted in this exact casing when
     * scanning free text ("C", "REST", "RAG", "AI"), to avoid matching ordinary English words.
     */
    private static final Map<String, List<String>> CASE_SENSITIVE_ALIASES = Map.of(
            "c", List.of("C"),
            "rest", List.of("REST"),
            "rag", List.of("RAG"),
            "artificial intelligence", List.of("AI")
    );

    /** Canonical names that must never be matched case-insensitively in free text. */
    private static final Set<String> CASE_SENSITIVE_ONLY = Set.of("c", "rest", "rag");

    private static final Pattern VERSION_SUFFIX = Pattern.compile("\\s+v?\\d+(\\.\\d+)*\\+?$");

    private static final String BEFORE = "(?<![\\p{L}\\p{N}+#.])";
    private static final String AFTER = "(?![\\p{L}\\p{N}+#])";

    /** lower-cased alias or canonical name -> canonical name */
    private static final Map<String, String> ALIAS_TO_CANONICAL = new HashMap<>();

    /** canonical name -> patterns that detect it in free text (insertion ordered) */
    private static final Map<String, List<Pattern>> DETECTORS = new LinkedHashMap<>();

    static {
        for (Skill s : SKILLS) {
            ALIAS_TO_CANONICAL.put(s.canonical(), s.canonical());
            List<Pattern> patterns = new ArrayList<>();
            if (!CASE_SENSITIVE_ONLY.contains(s.canonical())) {
                patterns.add(compile(s.canonical(), true));
            }
            for (String alias : s.aliases()) {
                ALIAS_TO_CANONICAL.put(alias.toLowerCase(Locale.ROOT), s.canonical());
                patterns.add(compile(alias, true));
            }
            DETECTORS.put(s.canonical(), patterns);
        }
        // "c" has no entry in SKILLS (it is only ever matched case-sensitively, see below)
        ALIAS_TO_CANONICAL.put("c", "c");
        ALIAS_TO_CANONICAL.put("ai", "artificial intelligence");

        CASE_SENSITIVE_ALIASES.forEach((canonical, aliases) -> {
            List<Pattern> patterns = DETECTORS.computeIfAbsent(canonical, k -> new ArrayList<>());
            aliases.forEach(alias -> patterns.add(compile(alias, false)));
        });
    }

    private SkillCatalog() {
    }

    private static Pattern compile(String alias, boolean caseInsensitive) {
        String[] words = alias.split(" ");
        StringBuilder body = new StringBuilder();
        for (int i = 0; i < words.length; i++) {
            if (i > 0) {
                body.append("[\\s-]+");
            }
            body.append(Pattern.quote(words[i]));
        }
        int flags = caseInsensitive ? Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE : 0;
        return Pattern.compile(BEFORE + body + AFTER, flags);
    }

    /**
     * Maps a skill name to its canonical lower-case form:
     * "Spring Boot" -> "spring", "React.js" -> "react", "Java 17" -> "java".
     * Unknown skills are returned trimmed and lower-cased.
     */
    public static String canonicalize(String skill) {

        if (skill == null) {
            return "";
        }

        String normalized = skill.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");

        String direct = ALIAS_TO_CANONICAL.get(normalized);
        if (direct != null) {
            return direct;
        }

        String withoutVersion = VERSION_SUFFIX.matcher(normalized).replaceFirst("");
        return ALIAS_TO_CANONICAL.getOrDefault(withoutVersion, withoutVersion);
    }

    public static Set<String> canonicalizeAll(Collection<String> skills) {

        Set<String> result = new LinkedHashSet<>();
        for (String skill : skills) {
            String canonical = canonicalize(skill);
            if (!canonical.isBlank()) {
                result.add(canonical);
            }
        }
        return result;
    }

    /** Canonical names of all catalog skills that appear as whole tokens in the text. */
    public static Set<String> detectIn(String text) {

        Set<String> found = new LinkedHashSet<>();

        if (text == null || text.isBlank()) {
            return found;
        }

        DETECTORS.forEach((canonical, patterns) -> {
            for (Pattern pattern : patterns) {
                if (pattern.matcher(text).find()) {
                    found.add(canonical);
                    break;
                }
            }
        });

        return found;
    }
}
