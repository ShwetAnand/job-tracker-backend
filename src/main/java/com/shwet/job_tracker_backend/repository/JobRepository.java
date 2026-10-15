package com.shwet.job_tracker_backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.shwet.job_tracker_backend.entity.Job;

/**
 * Every query used by the application is scoped to an owner. The old unscoped
 * finders (findAll, findByStatusIgnoreCase, search without user, ...) were removed
 * because they made cross-user reads one careless call away.
 */
public interface JobRepository extends JpaRepository<Job, Long>, JpaSpecificationExecutor<Job> {

    /** Ownership-checked lookup: empty when the job does not exist OR belongs to someone else. */
    @EntityGraph(attributePaths = "user")
    Optional<Job> findByIdAndUserId(Long id, Long userId);

    // user is always fetched in the same query so mapping to DTOs never triggers N+1 selects
    @Override
    @EntityGraph(attributePaths = "user")
    Page<Job> findAll(Specification<Job> spec, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = "user")
    List<Job> findAll(Specification<Job> spec, Sort sort);
}
