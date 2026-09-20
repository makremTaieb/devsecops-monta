import { Injectable } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Observable, catchError, throwError } from 'rxjs';
import { environment } from '../../../environments/environment';
import { CreateProjectRequest, Project } from '../models/project.model';
import { Pipeline } from '../models/pipeline.model';

@Injectable({ providedIn: 'root' })
export class ProjectService {

  // ✅ BASE = gateway route
private readonly baseUrl = `${environment.apiPipeline}/projects`;

  constructor(private http: HttpClient) {}

  // ================= PROJECTS =================

  getAll(): Observable<Project[]> {
    return this.http.get<Project[]>(this.baseUrl)
      .pipe(catchError(this.handleError));
  }

  getById(id: number): Observable<Project> {
    return this.http.get<Project>(`${this.baseUrl}/${id}`)
      .pipe(catchError(this.handleError));
  }

  
  create(req: CreateProjectRequest): Observable<Project> {
    return this.http.post<Project>(this.baseUrl, req)
      .pipe(catchError(this.handleError));
  }
  update(id: number, req: CreateProjectRequest): Observable<Project> {
    return this.http.put<Project>(`${this.baseUrl}/${id}`, req)
      .pipe(catchError(this.handleError));
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`)
      .pipe(catchError(this.handleError));
  }

  // ================= PIPELINES (inside project) =================

  getPipelines(projectId: number): Observable<Pipeline[]> {
    return this.http.get<Pipeline[]>(
      `${this.baseUrl}/${projectId}/pipelines`
    ).pipe(catchError(this.handleError));
  }

  // ================= ERROR HANDLER =================

  private handleError(error: HttpErrorResponse) {
    let msg = 'Erreur inconnue';

    if (error.status === 0) msg = 'Backend inaccessible';
    else if (error.status === 404) msg = 'Ressource introuvable';
    else if (error.status === 401) msg = 'Non autorisé';
    else if (error.status === 403) msg = 'Accès refusé';
    else if (error.error?.message) msg = error.error.message;

    console.error('[ProjectService]', error);
    return throwError(() => new Error(msg));
  }
}