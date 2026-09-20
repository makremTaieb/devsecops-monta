package com.example.pipelineservice.controller;

import com.example.pipelineservice.client.dto.request.CreatePipelineRequest;
import com.example.pipelineservice.client.dto.response.PipelineResponse;
import com.example.pipelineservice.service.PipelineService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/pipeline")
@RequiredArgsConstructor
public class PipelineController {

    private final PipelineService pipelineService;

    @PostMapping("/projects/{projectId}/pipelines")
    @ResponseStatus(HttpStatus.CREATED)
    public PipelineResponse createPipeline(
            @PathVariable Long projectId,
            @RequestBody CreatePipelineRequest request) {
        return pipelineService.createPipeline(projectId, request);
    }

    @GetMapping("/projects/{projectId}/pipelines")
    public List<PipelineResponse> getPipelinesByProject(
            @PathVariable Long projectId) {
        return pipelineService.getPipelinesByProject(projectId);
    }

    @GetMapping("/pipelines/{pipelineId}")
    public PipelineResponse getPipelineById(@PathVariable Long pipelineId) {
        return pipelineService.getPipelineById(pipelineId);
    }

    @PutMapping("/pipelines/{pipelineId}")
    public PipelineResponse updatePipeline(
            @PathVariable Long pipelineId,
            @RequestBody CreatePipelineRequest request) {
        return pipelineService.updatePipeline(pipelineId, request);
    }

    @DeleteMapping("/pipelines/{pipelineId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePipeline(@PathVariable Long pipelineId) {
        pipelineService.deletePipeline(pipelineId);
    }
}