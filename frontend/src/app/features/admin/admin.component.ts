import { Component, OnInit, OnDestroy, signal, inject, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';
import { AuthService } from '../../core/services/auth.service';
import { StatusCountPipe, StatusFilterPipe } from '../../core/pipes/status.pipe';
import { interval, Subscription } from 'rxjs';
import { catchError, of } from 'rxjs';

export interface AdminUser { id:number; username:string; email:string; role:string; enabled:boolean; createdAt:string; }
export interface PipelineExecution { id:number; pipelineId:number; commitHash:string; status:string; startTime:string; endTime:string; triggeredBy:string; jenkinsBuildNumber?:number; }
export interface AuditLog { id:number; action:string; resource:string; resourceId:number; details:string; status:string; sourceService:string; createdAt:string; username?:string; }
export interface ServiceHealth { name: string; label: string; up: boolean | null; }

@Component({
  selector: 'app-admin',
  standalone: true,
  imports: [CommonModule, FormsModule, StatusCountPipe, StatusFilterPipe],
  templateUrl: './admin.component.html',
  styleUrls: ['./admin.component.scss'],
})
export class AdminComponent implements OnInit, OnDestroy {

  private http = inject(HttpClient);
  private auth = inject(AuthService);
  private base = `${environment.apiAuth}/admin`;
  private execBase = `${environment.apiGateway}/api/executions`;
  private auditBase = `${environment.apiAudit}`;
  private prometheusBase = `${environment.apiPrometheus}`;

  users    = signal<AdminUser[]>([]);
  loading  = signal(true);
  error    = signal('');
  updating = signal<number | null>(null);
  readonly roles = ['ADMIN', 'DEV', 'DEVOPS', 'AUDITOR'];

  executions   = signal<PipelineExecution[]>([]);
  execLoading  = signal(true);
  auditLogs    = signal<AuditLog[]>([]);
  auditLoading = signal(true);

  services = signal<ServiceHealth[]>([
    { name: 'auth-service',         label: 'Auth Service',       up: null },
    { name: 'pipeline-service',     label: 'Pipeline Service',   up: null },
    { name: 'security-service',     label: 'Security Service',   up: null },
    { name: 'notification-service', label: 'Notification Svc',   up: null },
    { name: 'api-gateway',          label: 'API Gateway',        up: null },
    { name: 'audit-log-service',    label: 'Audit Log Service',  up: null },
  ]);
  healthLoading = signal(true);
  activeTab = signal<'users'|'builds'|'health'|'alerts'>('users');

  private refreshSub?: Subscription;

  get totalUsers()    { return this.users().length; }
  get activeUsers()   { return this.users().filter(u => u.enabled).length; }
  get disabledUsers() { return this.users().filter(u => !u.enabled).length; }
  get adminCount()    { return this.users().filter(u => u.role === 'ADMIN').length; }

  successRate = computed(() => {
    const e = this.executions();
    if (!e.length) return 0;
    return Math.round(e.filter(x => x.status === 'SUCCESS').length / e.length * 100);
  });
  failedBuilds  = computed(() => this.executions().filter(e => e.status === 'FAILED').length);
  runningBuilds = computed(() => this.executions().filter(e => e.status === 'RUNNING' || e.status === 'PENDING').length);
  servicesUp    = computed(() => this.services().filter(s => s.up === true).length);
  pipelineAlerts = computed(() => this.executions().filter(e => e.status === 'FAILED').slice(0,10));

  ngOnInit(): void {
    this.loadAll();
    this.refreshSub = interval(30000).subscribe(() => { this.loadExecutions(); this.loadHealth(); });
  }
  ngOnDestroy(): void { this.refreshSub?.unsubscribe(); }

  loadAll(): void { this.loadUsers(); this.loadExecutions(); this.loadAuditLogs(); this.loadHealth(); }

  loadUsers(): void {
    this.loading.set(true);
    this.http.get<AdminUser[]>(`${this.base}/users`).subscribe({
      next: data => { this.users.set(data); this.loading.set(false); },
      error: e   => { this.error.set(e?.error?.message ?? 'Erreur chargement'); this.loading.set(false); },
    });
  }

  loadExecutions(): void {
    this.execLoading.set(true);
    this.http.get<PipelineExecution[]>(`${this.execBase}`)
      .pipe(catchError(() => of([])))
      .subscribe(data => { this.executions.set(data); this.execLoading.set(false); });
  }

  loadAuditLogs(): void {
    this.auditLoading.set(true);
    this.http.get<AuditLog[]>(`${this.auditBase}/logs`)
      .pipe(catchError(() => of([])))
      .subscribe(data => { this.auditLogs.set(data.slice(0,20)); this.auditLoading.set(false); });
  }

  loadHealth(): void {
    this.healthLoading.set(true);
    this.http.get<any>(`${this.prometheusBase}/api/v1/query?query=up`)
      .pipe(catchError(() => of(null)))
      .subscribe(res => {
        if (res?.data?.result?.length) {
          this.services.update(svcs => svcs.map(svc => {
            const match = res.data.result.find((r: any) =>
              (r.metric?.job ?? r.metric?.app ?? r.metric?.application ?? '')
                .toLowerCase().includes(svc.name.replace(/-service/,'').replace(/-/g,'')));
            return { ...svc, up: match ? match.value[1] === '1' : null };
          }));
        }
        this.healthLoading.set(false);
      });
  }

  toggleEnabled(user: AdminUser): void {
    this.updating.set(user.id);
    this.http.put<AdminUser>(`${this.base}/users/${user.id}/enabled?enabled=${!user.enabled}`, {}).subscribe({
      next: u => { this.users.update(l => l.map(x => x.id === u.id ? u : x)); this.updating.set(null); },
      error: () => this.updating.set(null),
    });
  }

  changeRole(user: AdminUser, newRole: string): void {
    this.updating.set(user.id);
    this.http.put<AdminUser>(`${this.base}/users/${user.id}/role?role=${newRole}`, {}).subscribe({
      next: u => {
        this.users.update(l => l.map(x => x.id === u.id ? u : x));
        this.updating.set(null);
        if (u.username === this.auth.username) {
          this.auth.getMe().subscribe({ next: me => this.auth.updateCurrentUserRole(me.role), error: () => {} });
        }
      },
      error: () => this.updating.set(null),
    });
  }

  deleteUser(user: AdminUser): void {
    if (!confirm(`Supprimer "${user.username}" ?`)) return;
    this.updating.set(user.id);
    this.http.delete(`${this.base}/users/${user.id}`).subscribe({
      next:  () => { this.users.update(l => l.filter(x => x.id !== user.id)); this.updating.set(null); },
      error: () => this.updating.set(null),
    });
  }

  setTab(tab: 'users'|'builds'|'health'|'alerts'): void { this.activeTab.set(tab); }

  roleColor(role: string): string { const m: Record<string,string> = {ADMIN:'#7c3aed',DEV:'#2563eb',DEVOPS:'#059669',AUDITOR:'#d97706'}; return m[role]??'#6b7280'; }
  roleIcon(role: string):  string { const m: Record<string,string> = {ADMIN:'👑',DEV:'💻',DEVOPS:'🚀',AUDITOR:'🔍'}; return m[role]??'👤'; }
  statusColor(s: string): string { const m: Record<string,string> = {SUCCESS:'#16a34a',FAILED:'#dc2626',RUNNING:'#2563eb',PENDING:'#d97706',CANCELLED:'#6b7280',CREATED:'#9ca3af'}; return m[s]??'#6b7280'; }
  statusBg(s: string):    string { const m: Record<string,string> = {SUCCESS:'#dcfce7',FAILED:'#fee2e2',RUNNING:'#dbeafe',PENDING:'#fef3c7',CANCELLED:'#f3f4f6',CREATED:'#f9fafb'}; return m[s]??'#f3f4f6'; }
  statusIcon(s: string):  string { const m: Record<string,string> = {SUCCESS:'✅',FAILED:'❌',RUNNING:'⚡',PENDING:'⏳',CANCELLED:'⊘',CREATED:'○'}; return m[s]??'○'; }
  auditIcon(action: string): string {
    if (action.includes('LOGIN')) return '🔐';
    if (action.includes('PIPELINE')||action.includes('TRIGGER')) return '🚀';
    if (action.includes('DEPLOY')) return '📦';
    if (action.includes('DELETE')) return '🗑️';
    if (action.includes('CREATE')) return '✨';
    if (action.includes('UPDATE')||action.includes('ROLE')) return '✏️';
    return '📋';
  }
  duration(start: string, end: string): string {
    if (!start||!end) return '—';
    const ms = new Date(end).getTime()-new Date(start).getTime();
    if (ms<0) return '—';
    const s = Math.floor(ms/1000);
    return s<60 ? `${s}s` : `${Math.floor(s/60)}m ${s%60}s`;
  }
  trackById(_: number, item: any) { return item.id; }
}
