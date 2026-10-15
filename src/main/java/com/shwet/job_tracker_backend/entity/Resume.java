package com.shwet.job_tracker_backend.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "resumes", indexes = @Index(name = "idx_resumes_user_id", columnList = "user_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Resume {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Original uploaded PDF file name
    @Column(nullable = false)
    private String fileName;

    // Text extracted from PDF
    @Column(columnDefinition = "TEXT")
    private String rawText;

    // Skills extracted by AI
    @Column(columnDefinition = "TEXT")
    private String extractedSkills;

    // Resume upload timestamp
    @Column(nullable = false)
    private LocalDateTime uploadedAt;

    // When the resume chunks were last embedded into the vector store (RAG). Null = not indexed.
    private LocalDateTime indexedAt;

    // Resume belongs to a user
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
}