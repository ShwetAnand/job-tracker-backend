package com.shwet.job_tracker_backend.dto;

/** Paging/sorting/filter parameters for GET /api/jobs/page (all validated in the service). */
public record JobPageQuery(
        int page,
        int size,
        String sortBy,
        String direction,
        String keyword,
        String status) {
}
