export type SeverityLevel = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';
export type ScanType      = 'SAST' | 'SCA' | 'SECRET';

export interface Vulnerability {
  id:          number;
  type:        ScanType;
  severity:    SeverityLevel;
  description: string;
  filePath:    string;
  cve:         string;
}

// Matches backend ScanDetailResponse DTO exactly
// Backend returns: scanId, securityScore (not id/score)
export interface SecurityScan {
  scanId:          number;
  id:              number;       // alias — backend may return either
  projectId:       number;
  executionId:     number;
  securityScore:   number;
  score:           number;       // alias — backend may return either
  blocked:         boolean;
  createdAt:       string;
  vulnerabilities: Vulnerability[];
}