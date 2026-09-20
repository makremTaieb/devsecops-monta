import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { AuditPage } from '../models/audit.model';

@Injectable({ providedIn: 'root' })
export class AuditService {

  private base = `${environment.apiGateway}/api/audit`;

  constructor(private http: HttpClient) {}

  getLogs(filters: {
    username?: string;
    action?: string;
    resource?: string;
    status?: string;
    from?: string;
    to?: string;
    page?: number;
    size?: number;
  }): Observable<AuditPage> {
    let params = new HttpParams();
    if (filters.username)  params = params.set('username',  filters.username);
    if (filters.action)    params = params.set('action',    filters.action);
    if (filters.resource)  params = params.set('resource',  filters.resource);
    if (filters.status)    params = params.set('status',    filters.status);
    if (filters.from)      params = params.set('from',      filters.from);
    if (filters.to)        params = params.set('to',        filters.to);
    params = params.set('page', String(filters.page ?? 0));
    params = params.set('size', String(filters.size ?? 20));

    return this.http.get<AuditPage>(`${this.base}/logs`, { params });
  }
}
