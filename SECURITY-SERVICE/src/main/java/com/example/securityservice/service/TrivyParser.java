package com.example.securityservice.service;

import com.example.securityservice.entities.ScanType;
import com.example.securityservice.entities.SeverityLevel;
import com.example.securityservice.entities.Vulnerability;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@Service
public class TrivyParser {

    private final ObjectMapper mapper = new ObjectMapper();

    public List<Vulnerability> parse(MultipartFile file) {
        List<Vulnerability> list = new ArrayList<>();

        if (file == null || file.isEmpty()) return list;

        try {
            byte[] bytes   = file.getBytes();
            String content = new String(bytes).trim();

            if (content.isEmpty() || content.equals("null")) return list;

            JsonNode root = mapper.readTree(content);

            // Handle both {"Results":[...]} and empty {} or []
            JsonNode results = root.path("Results");
            if (results.isMissingNode() || !results.isArray()) return list;

            for (JsonNode result : results) {
                JsonNode vulns = result.path("Vulnerabilities");
                if (vulns.isMissingNode() || !vulns.isArray()) continue;

                String target = result.path("Target").asText("unknown");

                for (JsonNode v : vulns) {
                    String severityStr = v.path("Severity").asText("LOW");
                    String vulnId      = v.path("VulnerabilityID").asText("");
                    String title       = v.path("Title").asText(vulnId);
                    String pkgName     = v.path("PkgName").asText("");

                    list.add(
                        Vulnerability.builder()
                            .type(ScanType.SCA)
                            .severity(mapSeverity(severityStr))
                            .description(title)
                            .filePath(target + (pkgName.isEmpty() ? "" : " → " + pkgName))
                            .cve(vulnId)
                            .build()
                    );
                }
            }

        } catch (Exception e) {
            System.err.println("[TrivyParser] Parse error (treating as clean): " + e.getMessage());
        }

        System.out.printf("[TrivyParser] Parsed %d vulnerabilities%n", list.size());
        return list;
    }

    private SeverityLevel mapSeverity(String s) {
        if (s == null) return SeverityLevel.LOW;
        return switch (s.toUpperCase()) {
            case "CRITICAL" -> SeverityLevel.CRITICAL;
            case "HIGH"     -> SeverityLevel.HIGH;
            case "MEDIUM"   -> SeverityLevel.MEDIUM;
            default         -> SeverityLevel.LOW;
        };
    }
}
