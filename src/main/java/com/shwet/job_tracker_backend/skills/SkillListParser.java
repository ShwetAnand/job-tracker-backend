package com.shwet.job_tracker_backend.skills;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Turns the raw text an LLM returns for "list the skills" into a clean, bounded list.
 * LLM output is untrusted: it may contain markdown fences, bullets, headings,
 * whole sentences, duplicates or far too many items.
 */
public final class SkillListParser {

    public static final int MAX_SKILLS = 60;
    public static final int MAX_SKILL_LENGTH = 40;
    static final int MAX_WORDS = 5;

    private SkillListParser() {
    }

    public static List<String> parse(String raw) {

        if (raw == null || raw.isBlank()) {
            return List.of();
        }

        String text = stripCodeFences(raw);

        Map<String, String> unique = new LinkedHashMap<>();

        for (String part : text.split("[,\\n;|]")) {
            String cleaned = clean(part);
            if (isValidSkill(cleaned)) {
                unique.putIfAbsent(cleaned.toLowerCase(Locale.ROOT), cleaned);
                if (unique.size() >= MAX_SKILLS) {
                    break;
                }
            }
        }

        return new ArrayList<>(unique.values());
    }

    private static String stripCodeFences(String raw) {
        StringBuilder sb = new StringBuilder();
        for (String line : raw.split("\\R")) {
            String trimmed = line.trim();
            if (trimmed.startsWith("```")) {
                continue;
            }
            sb.append(line).append('\n');
        }
        return sb.toString();
    }

    private static String clean(String part) {

        String s = part.trim();

        // "Languages: Java" -> "Java"
        int colon = s.lastIndexOf(':');
        if (colon >= 0) {
            s = s.substring(colon + 1).trim();
        }

        // leading bullets / numbering
        s = s.replaceFirst("^(?:[-*\u2022\u00B7]+|\\d+[.)])\\s*", "");

        // surrounding quotes / brackets
        s = s.replaceAll("^[\\[\"'`]+|[\\]\"'`.]+$", "");

        return s.trim();
    }

    private static boolean isValidSkill(String s) {

        if (s.isBlank() || s.length() > MAX_SKILL_LENGTH) {
            return false;
        }
        if (s.split("\\s+").length > MAX_WORDS) {
            return false;
        }
        return s.chars().anyMatch(Character::isLetter);
    }
}
