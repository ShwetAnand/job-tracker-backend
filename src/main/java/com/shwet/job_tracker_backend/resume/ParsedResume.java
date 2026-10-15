package com.shwet.job_tracker_backend.resume;

/** A validated upload: safe display file name + extracted, cleaned text. */
public record ParsedResume(String fileName, String text) {
}
