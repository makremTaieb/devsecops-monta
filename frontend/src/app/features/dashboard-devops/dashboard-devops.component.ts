import { Component, OnInit, OnDestroy, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { forkJoin, interval, Subscription } from 'rxjs';

import { ProjectService }      from '../../core/services/project.service';
import { NotificationService } from '../../core/services/notification.service';
import { AuditService }        from '../../core/services/audit.service';
import { AuthService }         from '../../core/services/auth.service';
import { SecurityService }     from '../../core/services/security.service';
import { Project }             from '../../core/models/project.model';
import { NotificationItem }    from '../../core/models/notification.model';
import { AuditLog }            from '../../core/models/audit.model';
import { Pipeline }            from '../../core/models/pipeline.model';
import { SecurityScan }        from '../../core/models/security.model';
import { PipelineExecution }   from '../../core/models/pipeline.model';
import { PipelineService }     from '../../core/services/pipeline.service';

@Component({
  selector:    'app-dashboard-devops',
  standalone:  true,
  imports:     [CommonModule, RouterLink],
  templateUrl: './dashboard-devops.component.html',
  styleUrls:   ['./dashboard-devops.component.scss'],
})
export class DashboardDevopsComponent implements OnInit, OnDestroy {

  readonly auth = inject(AuthService);
  private projSvc     = inject(ProjectService);
  private notifSvc    = inject(NotificationService);
  private auditSvc    = inject(AuditService);
  private secSvc      = inject(SecurityService);
  private pipelineSvc = inject(PipelineService);

  projects      = signal<Project[]>([]);
  notifications = signal<NotificationItem[]>([]);
  pipelines     = signal<Pipeline[]>([]);
  executions    = signal<PipelineExecution[]>([]);
  auditLogs     = signal<AuditLog[]>([]);
  secScans      = signal<SecurityScan[]>([]);
  loading       = signal(true);
  lastRefresh   = signal<Date>(new Date());
  today         = new Date();

  projectTab = signal<'deployment' | 'pipelines'>('deployment');

  private pollSub?: Subscription;

  // ── KPIs ─────────────────────────────────────────────────────
  readonly kpis = computed(() => {
    const n = this.notifications(), p = this.projects();
    return {
      projects:     p.length,
      active:       p.filter(x => !x.status || x.status === 'ACTIVE').length,
      succeeded:    n.filter(x => x.eventType === 'PIPELINE_SUCCESS').length,
      failed:       n.filter(x => x.eventType === 'PIPELINE_FAILED').length,
      blocked:      n.filter(x => x.eventType === 'SECURITY_BLOCKED').length,
      warnings:     n.filter(x => x.eventType === 'SECURITY_WARNING').length,
      deployFailed: n.filter(x => x.eventType === 'DEPLOYMENT_FAILED').length,
      total_notif:  n.length,
      pipelines:    this.pipelines().length,
    };
  });

  readonly successRate = computed(() => {
    const k = this.kpis(), t = k.succeeded + k.failed;
    return t === 0 ? 100 : Math.round((k.succeeded / t) * 100);
  });

  readonly securityScore = computed(() => {
    const { blocked, total_notif } = this.kpis();
    return total_notif === 0 ? 100 : Math.max(0, Math.round(100 - (blocked / total_notif) * 100));
  });

  readonly deployFrequency = computed(() => {
    const s = this.notifications().filter(x => x.eventType === 'PIPELINE_SUCCESS');
    if (!s.length) return { value: 0, label: '0 / jour' };
    const v = parseFloat((s.length / 14).toFixed(1));
    return { value: v, label: `${v} / jour` };
  });

  // ── Vulnerability alerts by build (Trivy / OWASP / Sonar) ────
  readonly vulnByBuild = computed(() => {
    const scans = this.secScans();
    if (!scans.length) {
      // Fallback: generate from notifications
      return this.executions().slice(0, 8).map((e, i) => ({
        label: `#${e.jenkinsBuildNumber ?? (i+1)}`,
        trivyCrit: 0, trivyHigh: 0,
        owaspCrit: 0, owaspHigh: 0,
        sonarBugs: 0, sonarVuln: 0,
      }));
    }
    return scans.slice(0, 8).map((s, i) => {
      const vulns = s.vulnerabilities ?? [];
      return {
        label: `Scan${i+1}`,
        trivyCrit: vulns.filter(v => v.type === 'SCA' && v.severity === 'CRITICAL').length,
        trivyHigh: vulns.filter(v => v.type === 'SCA' && v.severity === 'HIGH').length,
        owaspCrit: vulns.filter(v => v.type === 'SAST' && v.severity === 'CRITICAL').length,
        owaspHigh: vulns.filter(v => v.type === 'SAST' && v.severity === 'HIGH').length,
        sonarBugs: vulns.filter(v => v.type === 'SECRET' && v.severity === 'MEDIUM').length,
        sonarVuln: vulns.filter(v => v.type === 'SECRET' && v.severity === 'LOW').length,
      };
    });
  });

  readonly maxVuln = computed(() => {
    const d = this.vulnByBuild();
    return Math.max(1, ...d.map(v => v.trivyCrit + v.trivyHigh + v.owaspCrit + v.owaspHigh + v.sonarBugs + v.sonarVuln));
  });

  // ── Build history chart (status + test coverage) ──────────────
  readonly buildHistoryChart = computed(() => {
    const execs = [...this.executions()]
      .sort((a, b) => new Date(b.startTime ?? 0).getTime() - new Date(a.startTime ?? 0).getTime())
      .slice(0, 12);
    return execs.map((e, i) => ({
      label: `#${e.jenkinsBuildNumber ?? (i+1)}`,
      ok:    e.status === 'SUCCESS',
      failed: e.status === 'FAILED',
      testCov: Math.round(Math.random() * 40 + 40), // placeholder until real coverage endpoint
      sonarCov: Math.round(Math.random() * 30 + 30),
    }));
  });

  // ── Service health (radar scores) ────────────────────────────
  readonly serviceHealth = computed(() => {
    const execs = this.executions();
    const stageScore = (name: string): number => {
      const relevant = execs.flatMap(e => e.stages?.filter(s => s.stageName?.toLowerCase().includes(name.toLowerCase())) ?? []);
      if (!relevant.length) return 75;
      const ok = relevant.filter(s => s.status === 'SUCCESS').length;
      return Math.round((ok / relevant.length) * 100);
    };
    return {
      build:  stageScore('build'),
      tests:  stageScore('test'),
      sonar:  stageScore('sonar'),
      owasp:  stageScore('owasp'),
      nexus:  stageScore('nexus'),
      deploy: stageScore('deploy'),
    };
  });

  // Radar chart SVG points
  readonly radarPoints = computed(() => {
    const h = this.serviceHealth();
    const scores = [h.build, h.tests, h.sonar, h.owasp, h.nexus, h.deploy];
    const cx = 90, cy = 90, r = 70;
    return scores.map((s, i) => {
      const angle = (i * 60 - 90) * Math.PI / 180;
      const sr = (s / 100) * r;
      return { x: cx + sr * Math.cos(angle), y: cy + sr * Math.sin(angle) };
    });
  });

  readonly radarPath = computed(() =>
    this.radarPoints().map((p, i) => `${i===0?'M':'L'}${p.x.toFixed(1)},${p.y.toFixed(1)}`).join(' ') + ' Z'
  );

  // ── Precomputed SVG point strings (arrow functions banned in templates) ──
  readonly radarPointsStr = computed(() =>
    this.radarPoints().map(p => p.x.toFixed(1) + ',' + p.y.toFixed(1)).join(' ')
  );

  readonly testCovPoints = computed(() => {
    const chart = this.buildHistoryChart();
    if (chart.length < 2) return '';
    const n = chart.length || 1;
    return chart.map((b, i) => ((i * (260 / n)) + 12).toFixed(1) + ',' + (120 - b.testCov).toFixed(1)).join(' ');
  });

  readonly sonarCovPoints = computed(() => {
    const chart = this.buildHistoryChart();
    if (chart.length < 2) return '';
    const n = chart.length || 1;
    return chart.map((b, i) => ((i * (260 / n)) + 12).toFixed(1) + ',' + (120 - b.sonarCov).toFixed(1)).join(' ');
  });

  readonly radarAxes = computed(() => {
    const labels = ['Build','Tests','Sonar','OWASP','Nexus','Deploy'];
    const cx = 90, cy = 90, r = 70;
    return labels.map((lbl, i) => {
      const angle = (i * 60 - 90) * Math.PI / 180;
      return { x: cx + r * Math.cos(angle), y: cy + r * Math.sin(angle), lbl };
    });
  });

  // ── Security alerts ───────────────────────────────────────────
  readonly securityAlerts = computed(() =>
    [...this.notifications()]
      .filter(n => n.eventType?.startsWith('SECURITY'))
      .sort((a, b) => new Date(b.createdAt ?? 0).getTime() - new Date(a.createdAt ?? 0).getTime())
      .slice(0, 8)
  );

  // ── Deployment events ─────────────────────────────────────────
  readonly deployEvents = computed(() =>
    [...this.notifications()]
      .filter(n => ['PIPELINE_SUCCESS','PIPELINE_FAILED','DEPLOYMENT_FAILED'].includes(n.eventType))
      .sort((a, b) => new Date(b.createdAt ?? 0).getTime() - new Date(a.createdAt ?? 0).getTime())
      .slice(0, 8)
  );

  // ── All notifications ─────────────────────────────────────────
  readonly recentNotifs = computed(() =>
    [...this.notifications()]
      .sort((a, b) => new Date(b.createdAt ?? 0).getTime() - new Date(a.createdAt ?? 0).getTime())
      .slice(0, 10)
  );

  // ── Audit logs ────────────────────────────────────────────────
  readonly recentLogs = computed(() =>
    [...this.auditLogs()]
      .sort((a, b) => new Date(b.timestamp ?? 0).getTime() - new Date(a.timestamp ?? 0).getTime())
      .slice(0, 8)
  );

  // ── Pipelines list ────────────────────────────────────────────
  readonly pipelinesSorted = computed(() =>
    [...this.pipelines()]
      .sort((a, b) => new Date(b.createdAt ?? 0).getTime() - new Date(a.createdAt ?? 0).getTime())
      .slice(0, 20)
  );

  // ── 14-day activity chart ─────────────────────────────────────
  readonly activityDays = computed(() => {
    const days: { label:string; success:number; failed:number; security:number; total:number }[] = [];
    for (let i = 13; i >= 0; i--) {
      const d = new Date(); d.setDate(d.getDate() - i);
      const ds = d.toISOString().slice(0, 10);
      const dn = this.notifications().filter(n => n.createdAt?.startsWith(ds));
      const success  = dn.filter(n => n.eventType === 'PIPELINE_SUCCESS').length;
      const failed   = dn.filter(n => n.eventType === 'PIPELINE_FAILED').length;
      const security = dn.filter(n => n.eventType?.startsWith('SECURITY')).length;
      days.push({ label: i===0?'Auj.':i===1?'Hier':d.toLocaleDateString('fr-FR',{day:'numeric',month:'short'}), success, failed, security, total: success+failed+security });
    }
    return days;
  });
  readonly maxDay    = computed(() => Math.max(1, ...this.activityDays().map(d => d.total)));
  readonly chartW    = 680; readonly chartH = 130; readonly barGap = 4;
  readonly barWidth  = computed(() => (this.chartW - 14 * this.barGap) / 14);
  readonly svgBars   = computed(() => {
    const days = this.activityDays(), max = this.maxDay(), bw = this.barWidth();
    return days.map((d, i) => {
      const x = i * (bw + this.barGap);
      const sucH  = max===0 ? 0 : (d.success  / max) * (this.chartH - 20);
      const failH = max===0 ? 0 : (d.failed   / max) * (this.chartH - 20);
      const secH  = max===0 ? 0 : (d.security / max) * (this.chartH - 20);
      return { x, bw, d, sucH, failH, secH };
    });
  });

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
        this.loadPipelines(projects);
        this.loading.set(false);
        this.lastRefresh.set(new Date());
      },
      error: () => this.loading.set(false),
    });
    this.auditSvc.getLogs({ size: 20 }).subscribe({ next: p => this.auditLogs.set(p.content ?? []), error: () => {} });
  }

  private loadPipelines(projects: Project[]): void {
    if (!projects.length) return;
    const all: Pipeline[] = []; let done = 0;
    projects.forEach(p => {
      this.projSvc.getPipelines(p.id).subscribe({
        next: pipes => {
          all.push(...pipes); done++;
          if (done === projects.length) {
            this.pipelines.set(all);
            this.loadExecutions(all);
            this.loadScans(projects);
          }
        },
        error: () => { done++; if (done === projects.length) this.pipelines.set(all); },
      });
    });
  }

  private loadExecutions(pipes: Pipeline[]): void {
    if (!pipes.length) return;
    const all: PipelineExecution[] = []; let done = 0;
    pipes.slice(0, 8).forEach(p => {
      this.pipelineSvc.getExecutions(p.id).subscribe({
        next: execs => { all.push(...execs); done++; if (done === Math.min(8, pipes.length)) this.executions.set(all); },
        error: () => { done++; if (done === Math.min(8, pipes.length)) this.executions.set(all); },
      });
    });
  }

  private loadScans(projects: Project[]): void {
    const all: SecurityScan[] = []; let done = 0;
    projects.slice(0, 5).forEach(p => {
      this.secSvc.getByProject(p.id).subscribe({
        next: scans => { all.push(...scans); done++; if (done === Math.min(5, projects.length)) this.secScans.set(all); },
        error: () => { done++; if (done === Math.min(5, projects.length)) this.secScans.set(all); },
      });
    });
  }

  setProjectTab(tab: 'deployment' | 'pipelines'): void { this.projectTab.set(tab); }

  eventIcon(type: string): string {
    const m: Record<string,string> = { PIPELINE_SUCCESS:'✓', PIPELINE_FAILED:'✕', DEPLOYMENT_FAILED:'⊗', SECURITY_BLOCKED:'🛡', SECURITY_WARNING:'⚠', UPDATE_PROJECT:'↻' };
    return m[type] ?? '•';
  }
  eventLabel(type: string): string {
    const m: Record<string,string> = { PIPELINE_SUCCESS:'Pipeline réussi', PIPELINE_FAILED:'Pipeline échoué', DEPLOYMENT_FAILED:'Déploiement échoué', SECURITY_BLOCKED:'Sécurité bloqué', SECURITY_WARNING:'Avertissement', UPDATE_PROJECT:'Mis à jour' };
    return m[type] ?? type;
  }
  pipelineStatusClass(s: string): string {
    if (s === 'SUCCESS') return 'st-success'; if (s === 'FAILED') return 'st-failed'; if (s === 'RUNNING') return 'st-running'; return 'st-pending';
  }
  logStatusClass(s: string): string { return s === 'SUCCESS' ? 'log-success' : s === 'FAILURE' ? 'log-fail' : 'log-info'; }
  pipelineName(pipelineId: number): string {
    return this.pipelines().find(p => p.id === pipelineId)?.name ?? 'Pipeline #' + pipelineId;
  }

  isSecurityBlocked(type: string): boolean { return type === 'SECURITY_BLOCKED'; }
  isSecurityWarning(type: string): boolean  { return type === 'SECURITY_WARNING'; }

  timeAgo(dateStr: string | null | undefined): string {
    if (!dateStr) return '—';
    const diff = Date.now() - new Date(dateStr).getTime(), m = Math.floor(diff / 60000);
    if (m < 1) return 'à l\'instant'; if (m < 60) return `il y a ${m}m`;
    const h = Math.floor(m / 60); if (h < 24) return `il y a ${h}h`; return `il y a ${Math.floor(h/24)}j`;
  }
  healthLabel(score: number): string {
    if (score >= 80) return '✅ Sain';
    if (score >= 50) return '⚠ Instable';
    return '❌ En échec';
  }
  healthClass(score: number): string {
    if (score >= 80) return 'health-good';
    if (score >= 50) return 'health-warn';
    return 'health-bad';
  }
}
