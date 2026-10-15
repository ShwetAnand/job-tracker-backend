package com.shwet.job_tracker_backend.resume;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.util.unit.DataSize;

/** Limits for uploaded resumes (app.resume.*). */
@ConfigurationProperties(prefix = "app.resume")
public record ResumeProperties(
        @DefaultValue("5MB") DataSize maxSize,
        @DefaultValue("20") int maxPages,
        @DefaultValue("50") int minTextChars,
        @DefaultValue("50000") int maxTextChars) {
}
