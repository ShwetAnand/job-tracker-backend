package com.shwet.job_tracker_backend.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import com.shwet.job_tracker_backend.entity.Resume;

public interface ResumeRepository extends JpaRepository<Resume, Long> {

    /** Ownership-checked lookup: empty when the resume does not exist OR belongs to someone else. */
    @EntityGraph(attributePaths = "user")
    Optional<Resume> findByIdAndUserId(Long id, Long userId);

    @EntityGraph(attributePaths = "user")
    List<Resume> findByUserIdOrderByUploadedAtDesc(Long userId);

    @Transactional
    @Modifying(clearAutomatically = true)
    @Query("update Resume r set r.indexedAt = :indexedAt where r.id = :id")
    void updateIndexedAt(@Param("id") Long id, @Param("indexedAt") LocalDateTime indexedAt);

    /** Targeted update (avoids merging a detached entity and rewriting the large text column). */
    @Transactional
    @Modifying(clearAutomatically = true)
    @Query("update Resume r set r.extractedSkills = :skills where r.id = :id")
    void updateExtractedSkills(@Param("id") Long id, @Param("skills") String skills);
}
