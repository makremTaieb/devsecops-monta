package com.example.pipelineservice.entities;

public enum PipelineStatus {
    CREATED,    // pipeline defined, never run
    PENDING,    // queued in Jenkins
    RUNNING,    // Jenkins build in progress
    SUCCESS,    // build passed + security passed
    FAILED,     // build failed OR security blocked
    CANCELLED
}