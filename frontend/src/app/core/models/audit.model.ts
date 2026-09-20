export type ActionType =
  | 'USER_LOGIN' | 'USER_LOGOUT' | 'USER_REGISTER' | 'USER_REFRESH_TOKEN'
  | 'USER_DELETED' | 'USER_UPDATED' | 'USER_ROLE_CHANGED'
  | 'PIPELINE_CREATED' | 'PIPELINE_UPDATED' | 'PIPELINE_DELETED'
  | 'PIPELINE_TRIGGERED' | 'PIPELINE_ABORTED'
  | 'PROJECT_CREATED' | 'PROJECT_UPDATED' | 'PROJECT_DELETED'
  | 'SECURITY_SCAN_TRIGGERED' | 'SECURITY_SCAN_COMPLETED' | 'SECURITY_SCAN_BLOCKED'
  | 'SYSTEM_EVENT';

export type ActionStatus = 'SUCCESS' | 'FAILURE';

export interface AuditLog {
  id: number;
  timestamp: string;
  userId: number | null;
  username: string | null;
  action: ActionType;
  resource: string | null;
  resourceId: number | null;
  details: string | null;
  ipAddress: string | null;
  status: ActionStatus;
  sourceService: string | null;
}

export interface AuditPage {
  content: AuditLog[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}
