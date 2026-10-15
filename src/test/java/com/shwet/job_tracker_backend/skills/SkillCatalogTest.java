package com.shwet.job_tracker_backend.skills;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

class SkillCatalogTest {

    @Test
    void javaIsNotDetectedInsideJavascript() {
        assertThat(SkillCatalog.detectIn("We use JavaScript and React")).contains("javascript", "react")
                .doesNotContain("java");
    }

    @Test
    void javaIsDetectedAsWholeWord() {
        assertThat(SkillCatalog.detectIn("Strong Java, Spring Boot experience.")).contains("java", "spring");
    }

    @Test
    void singleLetterCOnlyMatchesUppercaseToken() {
        assertThat(SkillCatalog.detectIn("We value cloud and teamwork, not much else")).doesNotContain("c");
        assertThat(SkillCatalog.detectIn("Languages: C, C++")).contains("c", "c++");
    }

    @Test
    void ordinaryWordsDoNotTriggerRestOrRag() {
        assertThat(SkillCatalog.detectIn("the rest of the team, an interest in storage and leverage"))
                .doesNotContain("rest", "rag", "git", "sql");
        assertThat(SkillCatalog.detectIn("Build REST APIs and a RAG pipeline")).contains("rest", "rag");
    }

    @Test
    void sqlVariantsAreNotConfused() {
        assertThat(SkillCatalog.detectIn("MySQL and PostgreSQL")).contains("mysql", "postgresql").doesNotContain("sql");
        assertThat(SkillCatalog.detectIn("Write SQL queries")).contains("sql");
    }

    @Test
    void multiWordAliasesAndHyphens() {
        assertThat(SkillCatalog.detectIn("object-oriented design, large language models, CI/CD"))
                .contains("oop", "llm", "ci/cd");
    }

    @Test
    void canonicalizeNormalisesNamesAndVersions() {
        assertThat(SkillCatalog.canonicalize("Spring Boot")).isEqualTo("spring");
        assertThat(SkillCatalog.canonicalize("React.js")).isEqualTo("react");
        assertThat(SkillCatalog.canonicalize("REST APIs")).isEqualTo("rest");
        assertThat(SkillCatalog.canonicalize("Java 17")).isEqualTo("java");
        assertThat(SkillCatalog.canonicalize("  Obscure Tool ")).isEqualTo("obscure tool");
        assertThat(SkillCatalog.canonicalizeAll(List.of("Java", "java 21", "JAVA"))).containsExactly("java");
    }

    @Test
    void nullAndBlankAreSafe() {
        assertThat(SkillCatalog.detectIn(null)).isEmpty();
        assertThat(SkillCatalog.detectIn("  ")).isEmpty();
        assertThat(SkillCatalog.canonicalize(null)).isEmpty();
    }
}
