import { Injectable } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Observable, catchError, throwError, map } from 'rxjs';
import { environment } from '../../../environments/environment';
import { SecurityScan } from '../models/security.model';

@Injectable({ providedIn: 'root' })
export class SecurityService {

  // apiSecurity = http://.../api/security  → calls become /api/security/scan/...
  private readonly base = `${environment.apiSecurity}`;

  constructor(private http: HttpClient) {}

  // GET /api/security/scan/project/{projectId}
  getByProject(projectId: number): Observable<SecurityScan[]> {
    return this.http.get<SecurityScan[]>(
      `${this.base}/scan/project/${projectId}`
    ).pipe(
      map(scans => scans.map(s => this.normalize(s))),
      catchError(this.handleError)
    );
  }

  // GET /api/security/scan/execution/{executionId}
  getByExecution(executionId: number): Observable<SecurityScan[]> {
    return this.http.get<any>(
      `${this.base}/scan/execution/${executionId}`
    ).pipe(
      // backend returns a single ScanDetailResponse here, wrap in array
      map(s => Array.isArray(s) ? s.map(i => this.normalize(i)) : [this.normalize(s)]),
      catchError(this.handleError)
    );
  }

  /** Normalize backend field names → frontend model aliases */
  private normalize(s: any): SecurityScan {
    return {
      ...s,
      scanId:        s.scanId        ?? s.id    ?? 0,
      id:            s.id            ?? s.scanId ?? 0,
      securityScore: s.securityScore ?? s.score  ?? 0,
      score:         s.score         ?? s.securityScore ?? 0,
      vulnerabilities: s.vulnerabilities ?? [],
    } as SecurityScan;
  }

  private handleError(error: HttpErrorResponse) {
    const msg = error.error?.message ?? `Erreur ${error.status}`;
    console.error('[SecurityService] Error:', error.status, msg);
    return throwError(() => new Error(msg));
  }
}
