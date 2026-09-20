// ---------- ScanOrchestrator.java ----------
package com.example.securityservice.service;

import com.example.securityservice.entities.Vulnerability;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ScanOrchestrator {

    private final SastScanner   sastScanner;
    private final ScaScanner    scaScanner;
    private final SecretScanner secretScanner;

    public List<Vulnerability> runAllScans(Long projectId) {

        List<Vulnerability> all = new ArrayList<>();

        all.addAll(sastScanner.scan(projectId));
        all.addAll(scaScanner.scan(projectId));
        all.addAll(secretScanner.scan(projectId));

        return all;
    }
}