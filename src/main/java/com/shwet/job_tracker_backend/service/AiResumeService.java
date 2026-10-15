package com.shwet.job_tracker_backend.service;

import java.util.List;

public interface AiResumeService {

    List<String> extractSkills(String resumeText);
}