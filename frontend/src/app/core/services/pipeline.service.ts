import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

import {
  Pipeline,
  PipelineExecution,
  CreatePipelineRequest,
  TriggerExecutionRequest
} from '../models/pipeline.model';

@Injectable({ providedIn: 'root' })
export class PipelineService {

  private base     = `${environment.apiPipeline}`;
  private execBase = `${environment.apiGateway}/api/executions`;

  constructor(private http: HttpClient) {}

  // ================= PIPELINES =================

  getByProject(projectId: number): Observable<Pipeline[]> {
    return this.http.get<Pipeline[]>(
      `${this.base}/projects/${projectId}/pipelines`
    );
  }

  getById(id: number): Observable<Pipeline> {
    return this.http.get<Pipeline>(
      `${this.base}/pipelines/${id}`
    );
  }

  create(projectId: number, req: CreatePipelineRequest): Observable<Pipeline> {
    return this.http.post<Pipeline>(
      `${this.base}/projects/${projectId}/pipelines`,
      req
    );
  }

  delete(pipelineId: number): Observable<void> {
    return this.http.delete<void>(
      `${this.base}/pipelines/${pipelineId}`
    );
  }

  // ================= EXECUTIONS =================
  // Backend endpoint: POST /api/executions/{pipelineId}

  getExecutions(pipelineId: number): Observable<PipelineExecution[]> {
    return this.http.get<PipelineExecution[]>(
      `${this.execBase}/pipeline/${pipelineId}`
    );
  }

  getExecution(executionId: number): Observable<PipelineExecution> {
    return this.http.get<PipelineExecution>(
      `${this.execBase}/${executionId}`
    );
  }

  /**
   * Trigger a pipeline execution (deploy).
   * Returns 202 ACCEPTED with status=PENDING.
   * Poll getExecution() to track progress.
   */
  trigger(
    pipelineId: number,
    req: TriggerExecutionRequest
  ): Observable<PipelineExecution> {
    return this.http.post<PipelineExecution>(
      `${this.execBase}/${pipelineId}`,
      req
    );
  }
}