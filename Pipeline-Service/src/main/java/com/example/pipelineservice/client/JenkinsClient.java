package com.example.pipelineservice.client;

import com.example.pipelineservice.entities.PipelineExecution;
import com.example.pipelineservice.entities.PipelineStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Component
@Slf4j
public class JenkinsClient {

    @Value("${jenkins.url}")
    private String jenkinsUrl;

    @Value("${jenkins.username}")
    private String jenkinsUser;

    @Value("${jenkins.token}")
    private String jenkinsToken;

    private final RestTemplate restTemplate = new RestTemplate();

    // ================= TRIGGER =================
    public String triggerJob(String jobName, PipelineExecution execution) {

        String url = jenkinsUrl + "/job/" + jobName + "/buildWithParameters";

        HttpHeaders headers = new HttpHeaders();
        headers.setBasicAuth(jenkinsUser, jenkinsToken);
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("EXECUTION_ID", execution.getId().toString());
        body.add("PROJECT_ID",   execution.getPipeline().getProject().getId().toString());
        body.add("COMMIT_HASH",  execution.getCommitHash());

        ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                new HttpEntity<>(body, headers),
                String.class
        );

        String queueUrl = response.getHeaders().getLocation().toString();
        log.info("[Jenkins] Queue URL: {}", queueUrl);

        return queueUrl;
    }

    // ================= QUEUE ID =================
    public String extractQueueId(String queueUrl) {

        String cleaned = queueUrl.replaceAll("/$", "");
        String[] parts = cleaned.split("/");

        for (int i = 0; i < parts.length - 1; i++) {
            if ("item".equals(parts[i])) {
                return parts[i + 1];
            }
        }

        throw new RuntimeException("Cannot extract queueId from: " + queueUrl);
    }

    // ================= BUILD NUMBER =================
    public Integer getBuildNumber(String queueId, String jobName) {

        try {
            String url = jenkinsUrl + "/queue/item/" + queueId + "/api/json";

            HttpHeaders headers = new HttpHeaders();
            headers.setBasicAuth(jenkinsUser, jenkinsToken);

            ResponseEntity<Map> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    Map.class
            );

            Map<?, ?> body = response.getBody();

            if (body != null && body.get("executable") instanceof Map<?, ?> exec) {
                Object number = exec.get("number");
                if (number != null) {
                    return ((Number) number).intValue();
                }
            }

        } catch (Exception ignored) {}

        return getLastBuild(jobName);
    }

    private Integer getLastBuild(String jobName) {

        try {
            String url = jenkinsUrl + "/job/" + jobName + "/lastBuild/api/json";

            HttpHeaders headers = new HttpHeaders();
            headers.setBasicAuth(jenkinsUser, jenkinsToken);

            ResponseEntity<Map> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    Map.class
            );

            Map<?, ?> body = response.getBody();

            if (body != null && body.get("number") != null) {
                return ((Number) body.get("number")).intValue();
            }

        } catch (Exception ignored) {}

        return null;
    }

    // ================= STATUS (FIXED: actually polls now) =================
    public PipelineStatus pollBuildStatus(String jobName, int buildNumber) {

        String url = jenkinsUrl + "/job/" + jobName + "/" + buildNumber + "/api/json";

        for (int i = 0; i < 30; i++) { // ✅ poll up to 30 times (~2.5 min)
            try {
                HttpHeaders headers = new HttpHeaders();
                headers.setBasicAuth(jenkinsUser, jenkinsToken);

                ResponseEntity<Map> response = restTemplate.exchange(
                        url,
                        HttpMethod.GET,
                        new HttpEntity<>(headers),
                        Map.class
                );

                Map<?, ?> body = response.getBody();

                if (body != null && Boolean.FALSE.equals(body.get("building"))) {
                    // ✅ Build finished — read the result
                    String result = (String) body.get("result");
                    PipelineStatus status = "SUCCESS".equals(result)
                            ? PipelineStatus.SUCCESS
                            : PipelineStatus.FAILED;

                    log.info("[Jenkins] Build {}/{} finished with result: {}", jobName, buildNumber, result);
                    return status;
                }

                log.info("[Jenkins] Build {}/{} still running... poll {}/30", jobName, buildNumber, i + 1);

            } catch (Exception e) {
                log.warn("[Jenkins] Poll error on attempt {}: {}", i, e.getMessage());
            }

            sleep(5000); // wait 5s between polls
        }

        log.error("[Jenkins] Timeout polling build {}/{}", jobName, buildNumber);
        return PipelineStatus.FAILED;
    }

    // ================= URL =================
    public String buildConsoleUrl(String jobName, int buildNumber) {
        return jenkinsUrl + "/job/" + jobName + "/" + buildNumber + "/console";
    }

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}