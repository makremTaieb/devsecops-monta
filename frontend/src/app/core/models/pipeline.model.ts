export type PipelineStatus =
  'CREATED' | 'RUNNING' | 'SUCCESS' | 'FAILED' | 'CANCELLED' | 'PENDING';

export type DeployEnvironment = 'Development (DEV)' | 'Staging (STG)' | 'Production (PROD)';

export interface Stage {
  id: number;
  name: string;
  orderIndex: number;
  type: string;
}

export interface Pipeline {
  id: number;
  name: string;
  projectId: number;
  jenkinsJobName: string;   // ← required by backend
  status: PipelineStatus;
  createdAt: string;
  stages: Stage[];
}

export interface StageExecution {
  stageId: number;
  stageName: string;
  stageType: string;
  status: PipelineStatus;
  startTime: string;
  endTime: string;
}

export interface PipelineExecution {
  id: number;
  pipelineId: number;
  commitHash: string;
  status: PipelineStatus;
  startTime: string;
  endTime: string;
  triggeredBy: string;
  jenkinsBuildNumber?: number;
  jenkinsBuildUrl?: string;
  jenkinsQueueId?: number;
  stages: StageExecution[];
}

/** POST /projects/{id}/pipelines */
export interface CreatePipelineRequest {
  name: string;
  jenkinsJobName: string;   // ← required by backend
}

/** POST /api/executions/{pipelineId} */
export interface TriggerExecutionRequest {
  commitHash: string;
  userId: string;   // backend ExecutionRequest.userId is String
}

/** Deploy modal form value */
export interface DeployFormValue {
  environment: DeployEnvironment;
  version: string;
  jenkinsJobName: string;
  commitHash: string;
}