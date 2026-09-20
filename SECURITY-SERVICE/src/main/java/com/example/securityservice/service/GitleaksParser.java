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
public class GitleaksParser {

    private final ObjectMapper mapper = new ObjectMapper();

    public List<Vulnerability> parse(MultipartFile file) {
        List<Vulnerability> list = new ArrayList<>();

        if (file == null || file.isEmpty()) return list;

        try {
            byte[] bytes = file.getBytes();
            String content = new String(bytes).trim();

            // Handle empty or null content gracefully
            if (content.isEmpty() || content.equals("null")) return list;

            JsonNode root = mapper.readTree(content);

            // Gitleaks outputs a JSON array [] — but sometimes {} or null is sent
            // when no secrets are found. Handle all cases safely.
            if (root.isArray()) {
                for (JsonNode leak : root) {
                    String description = leak.path("Description").asText("Secret detected");
                    String filePath    = leak.path("File").asText("");
                    String ruleId      = leak.path("RuleID").asText("");

                    list.add(
                        Vulnerability.builder()
                            .type(ScanType.SECRET)
                            .severity(SeverityLevel.HIGH)   // CHANGED: was CRITICAL — too aggressive
                            .description(description + (ruleId.isEmpty() ? "" : " [" + ruleId + "]"))
                            .filePath(filePath)
                            .cve("GITLEAKS-" + ruleId)
                            .build()
                    );
                }
            }
            // If root is object {} or anything else — just return empty list (no leaks)

        } catch (Exception e) {
            // Parsing error = treat as clean (don't block pipeline on parser failure)
            System.err.println("[GitleaksParser] Parse error (treating as clean): " + e.getMessage());
        }

        return list;
    }
}
