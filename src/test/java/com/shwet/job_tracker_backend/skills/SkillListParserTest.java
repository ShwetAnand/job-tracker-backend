package com.shwet.job_tracker_backend.skills;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SkillListParserTest {

    @Test
    void parsesCommaSeparatedList() {
        assertThat(SkillListParser.parse("Java, Spring Boot,  PostgreSQL ")).containsExactly("Java", "Spring Boot", "PostgreSQL");
    }

    @Test
    void handlesFencesBulletsHeadingsAndDuplicates() {
        String raw = "```\nSkills: Java\n- Spring Boot\n1. docker\n* java\n```";
        assertThat(SkillListParser.parse(raw)).containsExactly("Java", "Spring Boot", "docker");
    }

    @Test
    void handlesJsonArrayStyle() {
        assertThat(SkillListParser.parse("[\"Java\", \"Docker\"]")).containsExactly("Java", "Docker");
    }

    @Test
    void dropsSentencesAndOversizedItems() {
        String raw = "I could not find many skills in this resume because it is short, Java, " + "x".repeat(60);
        assertThat(SkillListParser.parse(raw)).containsExactly("Java");
    }

    @Test
    void capsNumberOfSkills() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 200; i++) {
            sb.append("skill").append(i).append(',');
        }
        assertThat(SkillListParser.parse(sb.toString())).hasSize(SkillListParser.MAX_SKILLS);
    }

    @Test
    void nullAndBlankGiveEmptyList() {
        assertThat(SkillListParser.parse(null)).isEmpty();
        assertThat(SkillListParser.parse("   ")).isEmpty();
    }
}
