import { Injectable } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Observable, catchError, throwError } from 'rxjs';
import { environment } from '../../../environments/environment';
import { NotificationItem } from '../models/notification.model';
 
@Injectable({ providedIn: 'root' })
export class NotificationService {

  private readonly base = `${environment.apiNotification}`;

  constructor(private http: HttpClient) {}

  getAll(): Observable<NotificationItem[]> {
    return this.http.get<NotificationItem[]>(this.base)
      .pipe(catchError(this.handleError));
  }

  getBySource(service: string): Observable<NotificationItem[]> {
    return this.http.get<NotificationItem[]>(`${this.base}/source/${service}`)
      .pipe(catchError(this.handleError));
  }

  private handleError(error: HttpErrorResponse) {
    const msg = error.error?.message ?? `Erreur ${error.status}`;
    console.error('[NotificationService] Error:', error.status, msg);
    return throwError(() => new Error(msg));
  }
}