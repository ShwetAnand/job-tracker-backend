package com.shwet.job_tracker_backend.repository.spec;

import java.util.Locale;

import org.springframework.data.jpa.domain.Specification;

import com.shwet.job_tracker_backend.entity.Job;

/** Composable filters for {@link Job}. Always combine with {@link #ownedBy(Long)}. */
public final class JobSpecifications {

    private JobSpecifications() {
    }

    public static Specification<Job> ownedBy(Long userId) {
        return (root, query, cb) -> cb.equal(root.get("user").get("id"), userId);
    }

    public static Specification<Job> statusEquals(String status) {
        String normalized = status.trim().toLowerCase(Locale.ROOT);
        return (root, query, cb) -> cb.equal(cb.lower(root.get("status")), normalized);
    }

    /** Case-insensitive "contains" on company name or role. LIKE wildcards in the input are escaped. */
    public static Specification<Job> keywordMatches(String keyword) {
        String pattern = "%" + escapeLike(keyword.trim().toLowerCase(Locale.ROOT)) + "%";
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("companyName")), pattern, '\\'),
                cb.like(cb.lower(root.get("role")), pattern, '\\'));
    }

    static String escapeLike(String input) {
        return input.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
