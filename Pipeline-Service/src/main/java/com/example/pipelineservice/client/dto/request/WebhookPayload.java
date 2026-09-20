package com.example.pipelineservice.client.dto.request;

import lombok.Data;

@Data
public class WebhookPayload {

    private String repositoryUrl;

    private String branch;

    private String commitHash;

    private String author;
}