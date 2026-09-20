import { Component, OnInit, OnDestroy, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { forkJoin, interval, Subscription } from 'rxjs';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';

import { ProjectService }      from '../../core/services/project.service';
import { NotificationService } from '../../core/services/notification.service';
import { AuthService }         from '../../core/services/auth.service';
import { SecurityService }     from '../../core/services/security.service';
import { Project }             from '../../core/models/project.model';
import { NotificationItem }    from '../../core/models/notification.model';
import { Pipeline, PipelineExecution } from '../../core/models/pipeline.model';
import { PipelineService }     from '../../core/services/pipeline.service';
import { SecurityScan }        from '../../core/models/security.model';

export interface AccountNode {
  id: string;
  label: string;
  type: 'root' | 'group' | 'user';
  icon: string;
  color: string;
  children?: AccountNode[];
  expanded?: boolean;
}

@Component({
  selector:    'app-dashboard-dev',
  standalone:  true,
  imports:     [CommonModule, RouterLink],
  templateUrl: './dashboard-dev.component.html',
  styleUrls:   ['./dashboard-dev.component.scss'],
})
export class DashboardDevComponent implements OnInit, OnDestroy {

  readonly auth = inject(AuthService);
  private projSvc     = inject(ProjectService);
  private notifSvc    = inject(NotificationService);
  private pipelineSvc = inject(PipelineService);
  private secSvc      = inject(SecurityService);
  private http        = inject(HttpClient);
  private adminBase   = `${environment.apiAuth}/admin`;

  projects      = signal<Project[]>([]);
  notifications = signal<NotificationItem[]>([]);
  pipelines     = signal<Pipeline[]>([]);
  executions    = signal<PipelineExecution[]>([]);
  secScans      = signal<SecurityScan[]>([]);
  loading       = signal(true);
  lastRefresh   = signal<Date>(new Date());
  today         = new Date();

  deployAnimating = signal(false);
  deploySuccess   = signal<boolean | null>(null);

  private pollSub?: Subscription;

  // ── KPIs ─────────────────────────────────────────────────────
  readonly kpis = computed(() => {
    const n = this.notifications(), p = this.projects();
    return {
      projects:  p.length,
      active:    p.filter(x => !x.status || x.status === 'ACTIVE').length,
      succeeded: n.filter(x => x.eventType === 'PIPELINE_SUCCESS').length,
      failed:    n.filter(x => x.eventType === 'PIPELINE_FAILED').length,
      blocked:   n.filter(x => x.eventType === 'SECURITY_BLOCKED').length,
      total:     n.length,
      pipelines: this.pipelines().length,
    };
  });

  readonly successRate = computed(() => {
    const k = this.kpis(), t = k.succeeded + k.failed;
    return t === 0 ? 100 : Math.round((k.succeeded / t) * 100);
  });

  // ── Donut ─────────────────────────────────────────────────────
  readonly donutR      = 42;
  readonly donutCirc   = computed(() => 2 * Math.PI * this.donutR);
  readonly donutOffset = computed(() => this.donutCirc() * (1 - this.successRate() / 100));

  // ── Test results (derived from executions stage data) ─────────
  readonly testResults = computed(() => {
    const execs = this.executions();
    let passed = 0, failed = 0, ignored = 0;
    execs.forEach(e => {
      const testStage = e.stages?.find(s => s.stageType === 'TEST' || s.stageName?.toLowerCase().includes('test'));
      if (testStage) {
        if (testStage.status === 'SUCCESS') passed++;
        else if (testStage.status === 'FAILED') failed++;
        else ignored++;
      }
    });
    // Fallback: use pipeline success/fail notifications as proxy
    if (passed === 0 && failed === 0) {
      passed  = this.kpis().succeeded;
      failed  = this.kpis().failed;
      ignored = 0;
    }
    const total = passed + failed + ignored;
    const rate  = total === 0 ? 0 : Math.round((passed / total) * 100);
    return { passed, failed, ignored, total, rate };
  });

  // ── Test coverage per build (last 5 executions) ───────────────
  readonly buildCoverage = computed(() => {
    const execs = [...this.executions()]
      .sort((a, b) => new Date(b.startTime ?? 0).getTime() - new Date(a.startTime ?? 0).getTime())
      .slice(0, 5);
    return execs.map((e, i) => {
      const stages: Record<string, string> = {};
      ['Build', 'Deploy', 'Tests', 'Sonar', 'OWASP', 'Nexus'].forEach(name => {
        const s = e.stages?.find(st => st.stageName?.toLowerCase().includes(name.toLowerCase()));
        stages[name] = s ? s.status : '—';
      });
      return { buildNum: `#${e.jenkinsBuildNumber ?? (i + 1)}`, stages };
    });
  });

  // ── Build history ─────────────────────────────────────────────
  readonly buildHistory = computed(() =>
    [...this.notifications()]
      .filter(n => ['PIPELINE_SUCCESS','PIPELINE_FAILED','DEPLOYMENT_FAILED'].includes(n.eventType))
      .sort((a, b) => new Date(b.createdAt ?? 0).getTime() - new Date(a.createdAt ?? 0).getTime())
      .slice(0, 5)
  );

  // ── Pipeline stage tree ───────────────────────────────────────
  readonly pipelineStageTree = computed(() =>
    this.pipelines().slice(0, 6).map(p => {
      const execs = this.executions().filter(e => e.pipelineId === p.id);
      const last  = execs.sort((a, b) => new Date(b.startTime ?? 0).getTime() - new Date(a.startTime ?? 0).getTime())[0];
      return { pipeline: p, lastExec: last ?? null };
    })
  );

  // ── Notification feed ─────────────────────────────────────────
  readonly notifFeed = computed(() =>
    [...this.notifications()]
      .sort((a, b) => new Date(b.createdAt ?? 0).getTime() - new Date(a.createdAt ?? 0).getTime())
      .slice(0, 10)
  );

  // ── Recent projects ───────────────────────────────────────────
  readonly recentProjects = computed(() =>
    [...this.projects()]
      .sort((a, b) => new Date(b.createdAt ?? 0).getTime() - new Date(a.createdAt ?? 0).getTime())
      .slice(0, 5)
  );

  // ── Account tree (loaded from API) ───────────────────────────
  readonly accountTree = signal<AccountNode[]>([{
    id: 'root', label: 'Organisation STB', type: 'root', icon: '🏢', color: '#6366f1', expanded: true,
    children: [
      { id: 'admins', label: 'Administrateurs', type: 'group', icon: '👑', color: '#7c3aed', expanded: true, children: [] },
      { id: 'devops', label: 'Équipe DevOps',   type: 'group', icon: '⚙️', color: '#059669', expanded: false, children: [] },
      { id: 'devs',   label: 'Développeurs',    type: 'group', icon: '💻', color: '#2563eb', expanded: false, children: [] },
    ]
  }]);

  private buildAccountTree(users: {id:number;username:string;role:string;enabled:boolean}[]): void {
    const make = (u: {id:number;username:string;role:string}, color: string): AccountNode =>
      ({ id: String(u.id), label: u.username, type: 'user', icon: '👤', color });

    const admins = users.filter(u => u.role === 'ADMIN').map(u => make(u, '#8b5cf6'));
    const devops = users.filter(u => u.role === 'DEVOPS').map(u => make(u, '#10b981'));
    const devs   = users.filter(u => u.role === 'DEV').map(u => make(u, '#3b82f6'));

    this.accountTree.set([{
      id: 'root', label: 'Organisation STB', type: 'root', icon: '🏢', color: '#6366f1', expanded: true,
      children: [
        { id: 'admins', label: 'Administrateurs', type: 'group', icon: '👑', color: '#7c3aed', expanded: true,  children: admins },
        { id: 'devops', label: 'Équipe DevOps',   type: 'group', icon: '⚙️', color: '#059669', expanded: false, children: devops },
        { id: 'devs',   label: 'Développeurs',    type: 'group', icon: '💻', color: '#2563eb', expanded: false, children: devs   },
      ]
    }]);
  }

  private loadUsers(): void {
    this.http.get<{id:number;username:string;role:string;enabled:boolean}[]>(`${this.adminBase}/users`)
      .subscribe({ next: users => this.buildAccountTree(users), error: () => {} });
  }

  ngOnInit(): void {
    this.loadAll();
    this.pollSub = interval(30_000).subscribe(() => this.loadAll(false));
  }
  ngOnDestroy(): void { this.pollSub?.unsubscribe(); }

  loadAll(showLoader = true): void {
    if (showLoader) this.loading.set(true);
    forkJoin({ projects: this.projSvc.getAll(), notifications: this.notifSvc.getAll() }).subscribe({
      next: ({ projects, notifications }) => {
        this.projects.set(projects);
        this.notifications.set(notifications);
        this.loadPipelinesAndExecs(projects);
        this.loadUsers();
        this.loading.set(false);
        this.lastRefresh.set(new Date());
      },
      error: () => this.loading.set(false),
    });
  }

  private loadPipelinesAndExecs(projects: Project[]): void {
    if (!projects.length) return;
    const allPipes: Pipeline[] = []; let done = 0;
    projects.forEach(p => {
      this.projSvc.getPipelines(p.id).subscribe({
        next: pipes => {
          allPipes.push(...pipes); done++;
          if (done === projects.length) {
            this.pipelines.set(allPipes);
            this.loadExecutions(allPipes);
            this.loadScans(projects);
          }
        },
        error: () => { done++; if (done === projects.length) this.pipelines.set(allPipes); },
      });
    });
  }

  private loadExecutions(pipes: Pipeline[]): void {
    if (!pipes.length) return;
    const allExecs: PipelineExecution[] = []; let done = 0;
    pipes.slice(0, 6).forEach(p => {
      this.pipelineSvc.getExecutions(p.id).subscribe({
        next: execs => { allExecs.push(...execs); done++; if (done === Math.min(6, pipes.length)) this.executions.set(allExecs); },
        error: () => { done++; if (done === Math.min(6, pipes.length)) this.executions.set(allExecs); },
      });
    });
  }

  private loadScans(projects: Project[]): void {
    const allScans: SecurityScan[] = []; let done = 0;
    projects.slice(0, 3).forEach(p => {
      this.secSvc.getByProject(p.id).subscribe({
        next: scans => { allScans.push(...scans); done++; if (done === Math.min(3, projects.length)) this.secScans.set(allScans); },
        error: () => { done++; if (done === Math.min(3, projects.length)) this.secScans.set(allScans); },
      });
    });
  }

  triggerRocketDeploy(project: Project): void {
    if (this.deployAnimating()) return;
    this.deployAnimating.set(true);
    this.deploySuccess.set(null);
    setTimeout(() => {
      this.deployAnimating.set(false);
      this.deploySuccess.set(true);
      setTimeout(() => this.deploySuccess.set(null), 3000);
    }, 2500);
  }

  toggleNode(node: AccountNode): void {
    node.expanded = !node.expanded;
    this.accountTree.update(t => [...t]);
  }

  stageStatusClass(status: string): string {
    if (status === 'SUCCESS')   return 'stage-ok';
    if (status === 'FAILED')    return 'stage-fail';
    if (status === 'RUNNING')   return 'stage-run';
    if (status === 'CANCELLED') return 'stage-cancel';
    return 'stage-pending';
  }
  stageStatusIcon(status: string): string {
    if (status === 'SUCCESS')   return '✓';
    if (status === 'FAILED')    return '✕';
    if (status === 'RUNNING')   return '⟳';
    if (status === 'CANCELLED') return '⊘';
    return '○';
  }
  buildStatusClass(type: string): string {
    if (type === 'PIPELINE_SUCCESS') return 'build-ok';
    if (type === 'PIPELINE_FAILED' || type === 'DEPLOYMENT_FAILED') return 'build-fail';
    return 'build-warn';
  }
  buildStatusLabel(type: string): string {
    if (type === 'PIPELINE_SUCCESS')  return 'RÉUSSI';
    if (type === 'PIPELINE_FAILED')   return 'ÉCHEC';
    if (type === 'DEPLOYMENT_FAILED') return 'DÉPL. ÉCHOUÉ';
    return 'INFO';
  }
  stageColClass(status: string): string {
    if (status === 'SUCCESS')   return 'cov-ok';
    if (status === 'FAILED')    return 'cov-fail';
    if (status === 'RUNNING')   return 'cov-run';
    if (status === '—')         return 'cov-na';
    return 'cov-pending';
  }
  eventIcon(type: string): string {
    const m: Record<string,string> = { PIPELINE_SUCCESS:'✓', PIPELINE_FAILED:'✕', DEPLOYMENT_FAILED:'⊗', SECURITY_BLOCKED:'🛡', SECURITY_WARNING:'⚠', UPDATE_PROJECT:'↻' };
    return m[type] ?? '•';
  }
  eventLabel(type: string): string {
    const m: Record<string,string> = { PIPELINE_SUCCESS:'Pipeline réussi', PIPELINE_FAILED:'Pipeline échoué', DEPLOYMENT_FAILED:'Déploiement échoué', SECURITY_BLOCKED:'Alerte sécurité', SECURITY_WARNING:'Avertissement', UPDATE_PROJECT:'Projet mis à jour' };
    return m[type] ?? type;
  }
  timeAgo(dateStr: string | null | undefined): string {
    if (!dateStr) return '—';
    const diff = Date.now() - new Date(dateStr).getTime(), m = Math.floor(diff / 60000);
    if (m < 1) return 'à l\'instant'; if (m < 60) return `il y a ${m}m`;
    const h = Math.floor(m / 60); if (h < 24) return `il y a ${h}h`; return `il y a ${Math.floor(h/24)}j`;
  }
}
