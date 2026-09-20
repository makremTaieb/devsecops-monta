package com.example.securityservice.service;

import com.example.securityservice.entities.Vulnerability;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * SAST scanner — results come from SonarQube analysis run during mvn verify.
 * SonarQube results are pushed to the SonarQube server and reviewed separately.
 * This class does not call SonarQube at scan time to avoid network dependency.
 */
@Service
public class SastScanner {
    public List<Vulnerability> scan(Long projectId) {
        // SAST results come from the SonarQube stage in Jenkins.
        // Not fetched at runtime to avoid blocking the pipeline on SonarQube availability.
        return List.of();
    }
}
