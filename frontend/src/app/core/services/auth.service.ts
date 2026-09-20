import { Injectable, signal, computed, inject, PLATFORM_ID } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, tap } from 'rxjs';
import { environment } from '../../../environments/environment';

import {
  AuthResponse,
  LoginRequest,
  RegisterRequest,
  RefreshTokenRequest,
  UserResponse
} from '../models/auth.model';

@Injectable({ providedIn: 'root' })
export class AuthService {

  private platformId = inject(PLATFORM_ID);

  private _currentUser = signal<AuthResponse | null>(null);

  // public state
  currentUser = this._currentUser.asReadonly();
  isLoggedIn = computed(() => !!this._currentUser());

  constructor(
    private http: HttpClient,
    private router: Router
  ) {
    this.restoreSession();
  }

  // ================= STORAGE SAFE =================
  private read(key: string): string | null {
    if (!isPlatformBrowser(this.platformId)) return null;
    return localStorage.getItem(key);
  }

  private write(key: string, value: string): void {
    if (!isPlatformBrowser(this.platformId)) return;
    localStorage.setItem(key, value);
  }

  private clear(): void {
    if (!isPlatformBrowser(this.platformId)) return;
    localStorage.removeItem('currentUser');
    localStorage.removeItem('accessToken');
  }

  private restoreSession(): void {
    if (!isPlatformBrowser(this.platformId)) return;
    const user = this.read('currentUser');
    if (user) {
      this._currentUser.set(JSON.parse(user));
      // Refresh role from server in background so it's always up-to-date
      this.refreshRoleFromServer();
    }
  }

  /** Calls /me and updates stored role if it changed (e.g. admin changed your role). */
  private refreshRoleFromServer(): void {
    this.getMe().subscribe({
      next: (me) => {
        const current = this._currentUser();
        if (current && me.role !== current.role) {
          const updated = { ...current, role: me.role };
          this.write('currentUser', JSON.stringify(updated));
          this._currentUser.set(updated);
        }
      },
      error: () => { /* silently ignore — offline or token expired */ }
    });
  }

  // ================= AUTH API (FIXED) =================

  login(req: LoginRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(
      `${environment.apiAuth}/login`,
      req
    ).pipe(tap(res => this.persist(res)));
  }

  register(req: RegisterRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(
      `${environment.apiAuth}/register`,   // ✅ FIXED
      req
    ).pipe(tap(res => this.persist(res)));
  }

  refresh(req: RefreshTokenRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(
      `${environment.apiAuth}/refresh`,
      req
    ).pipe(tap(res => this.persist(res)));
  }

  getMe(): Observable<UserResponse> {
    return this.http.get<UserResponse>(
      `${environment.apiAuth}/me`
    );
  }

  // ================= LOGOUT =================
  logout(): void {
    this.clear();
    this._currentUser.set(null);
    this.router.navigate(['/auth/login']);
  }

  // ================= GETTERS =================
  get token(): string | null {
    return this.read('accessToken');
  }

  get username(): string {
    return this._currentUser()?.username ?? '';
  }

  get role(): string {
    return this._currentUser()?.role ?? '';
  }

  // ================= UPDATE ROLE =================
  /** Called when admin changes a user's role while they're logged in. */
  updateCurrentUserRole(newRole: string): void {
    const cur = this._currentUser();
    if (!cur) return;
    const updated = { ...cur, role: newRole };
    this.write('currentUser', JSON.stringify(updated));
    this._currentUser.set(updated);
  }

  // ================= SAVE SESSION =================
  private persist(res: AuthResponse): void {
    this.write('currentUser', JSON.stringify(res));
    this.write('accessToken', res.accessToken);
    this._currentUser.set(res);
  }
}