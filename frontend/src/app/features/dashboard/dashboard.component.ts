import { Component, OnInit, OnDestroy, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { forkJoin, interval, Subscription } from 'rxjs';
import { ProjectService }      from '../../core/services/project.service';
import { NotificationService } from '../../core/services/notification.service';
import { AuthService }         from '../../core/services/auth.service';
import { Project }             from '../../core/models/project.model';
import { NotificationItem }    from '../../core/models/notification.model';
import { environment }         from '../../../environments/environment';

export interface AdminUser { id:number; username:string; email:string; role:string; enabled:boolean; createdAt:string; }

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.scss'],
})
export class DashboardComponent implements OnInit, OnDestroy {

  readonly auth = inject(AuthService);
  private  http = inject(HttpClient);
  private  projSvc  = inject(ProjectService);
  private  notifSvc = inject(NotificationService);

  projects      = signal<Project[]>([]);
  notifications = signal<NotificationItem[]>([]);
  users         = signal<AdminUser[]>([]);
  loading       = signal(true);
  lastRefresh   = signal<Date>(new Date());
  today         = new Date();

  private pollSub?: Subscription;

  // ── Computed KPIs ───────────────────────────────────────────
  readonly kpis = computed(() => {
    const n = this.notifications(), p = this.projects(), u = this.users();
    return {
      projects:    p.length,
      active:      p.filter(x => !x.status || x.status === 'ACTIVE').length,
      succeeded:   n.filter(x => x.eventType === 'PIPELINE_SUCCESS').length,
      failed:      n.filter(x => x.eventType === 'PIPELINE_FAILED').length,
      blocked:     n.filter(x => x.eventType === 'SECURITY_BLOCKED').length,
      warnings:    n.filter(x => x.eventType === 'SECURITY_WARNING').length,
      total_notif: n.length,
      totalUsers:  u.length,
      activeUsers: u.filter(x => x.enabled).length,
      blockedUsers:u.filter(x => !x.enabled).length,
      admins:      u.filter(x => x.role === 'ADMIN').length,
      devs:        u.filter(x => x.role === 'DEV').length,
      devops:      u.filter(x => x.role === 'DEVOPS').length,
    };
  });

  readonly successRate = computed((): number => {
    const k = this.kpis(); const total = k.succeeded + k.failed;
    return total === 0 ? 100 : Math.round((k.succeeded / total) * 100);
  });

  readonly activityDays = computed(() => {
    const days: any[] = [];
    for (let i = 13; i >= 0; i--) {
      const d = new Date(); d.setDate(d.getDate() - i);
      const ds = d.toISOString().slice(0, 10);
      const dn = this.notifications().filter(n => n.createdAt?.startsWith(ds));
      const success = dn.filter(n => n.eventType === 'PIPELINE_SUCCESS').length;
      const failed  = dn.filter(n => n.eventType === 'PIPELINE_FAILED').length;
      const security= dn.filter(n => n.eventType?.startsWith('SECURITY')).length;
      days.push({ label: i===0?'Auj.':i===1?'Hier':d.toLocaleDateString('fr-FR',{day:'numeric',month:'short'}), success, failed, security, total: success+failed+security });
    }
    return days;
  });

  readonly maxDayCount = computed((): number => Math.max(1, ...this.activityDays().map((d:any)=>d.total)));
  readonly chartW = 700; readonly chartH = 160; readonly barGap = 4;
  readonly barWidth = computed(() => (this.chartW - (14 * this.barGap)) / 14);

  svgBars = computed(() => {
    const days = this.activityDays(), max = this.maxDayCount(), bw = this.barWidth();
    return days.map((d:any, i:number) => {
      const x = i * (bw + this.barGap);
      const sucH  = max===0 ? 0 : (d.success / max) * (this.chartH - 20);
      const failH = max===0 ? 0 : (d.failed  / max) * (this.chartH - 20);
      const secH  = max===0 ? 0 : (d.security/ max) * (this.chartH - 20);
      const totalH= sucH+failH+secH;
      return { x, bw, d, totalH, sucH, failH, secH, y: this.chartH - 20 - totalH };
    });
  });

  readonly donutSegments = computed(() => {
    const k = this.kpis(); const total = k.succeeded+k.failed+k.blocked+k.warnings;
    if (!total) return [];
    const r = 54, circ = 2*Math.PI*r;
    const items = [
      {label:'Succès', value:k.succeeded, color:'#16a34a'},
      {label:'Échecs', value:k.failed,    color:'#dc2626'},
      {label:'Bloqués',value:k.blocked,   color:'#d97706'},
      {label:'Alertes',value:k.warnings,  color:'#6366f1'},
    ].filter(s=>s.value>0);
    let offset=0;
    return items.map(seg => { const dash=(seg.value/total)*circ, gap=circ-dash, result={...seg,dash,gap,offset}; offset+=dash; return result; });
  });

  readonly sparklinePoints = computed(() => {
    return this.activityDays().map((d:any, i:number) => {
      const total = d.success+d.failed, rate = total===0?100:Math.round((d.success/total)*100);
      return `${i*(100/13)},${100-rate}`;
    }).join(' ');
  });

  readonly recentNotifications = computed(() =>
    [...this.notifications()].sort((a,b)=>new Date(b.createdAt).getTime()-new Date(a.createdAt).getTime()).slice(0, 8)
  );
  readonly recentProjects = computed(() =>
    [...this.projects()].sort((a,b)=>new Date(b.createdAt).getTime()-new Date(a.createdAt).getTime()).slice(0, 5)
  );
  readonly blockedUsers = computed(() => this.users().filter(u => !u.enabled));
  readonly recentUsers  = computed(() => [...this.users()].sort((a,b)=>new Date(b.createdAt).getTime()-new Date(a.createdAt).getTime()).slice(0,5));

  readonly deployFrequency = computed(() => {
    const s = this.notifications().filter(x=>x.eventType==='PIPELINE_SUCCESS');
    if (!s.length) return {value:0, label:'Aucun déploiement'};
    const v = parseFloat((s.length/14).toFixed(1));
    return {value:v, label:`${v} / jour`};
  });
  readonly changeFailRate = computed(() => {
    const k=this.kpis(), total=k.succeeded+k.failed;
    return total===0 ? '0%' : Math.round((k.failed/total)*100)+'%';
  });
  readonly securityScore = computed(() => {
    const b=this.kpis().blocked, t=this.kpis().total_notif;
    return t===0 ? 100 : Math.max(0, Math.round(100-(b/t)*100));
  });

  // role helpers
  get isAdmin()  { return this.auth.role === 'ADMIN';  }
  get isDevops() { return this.auth.role === 'DEVOPS'; }
  get isDev()    { return this.auth.role === 'DEV';    }

  ngOnInit(): void { this.loadAll(); this.pollSub = interval(30_000).subscribe(() => this.loadAll(false)); }
  ngOnDestroy(): void { this.pollSub?.unsubscribe(); }

  loadAll(showLoader = true): void {
    if (showLoader) this.loading.set(true);
    const calls: any = { projects: this.projSvc.getAll(), notifications: this.notifSvc.getAll() };
    if (this.isAdmin) {
      calls['users'] = this.http.get<AdminUser[]>(`${environment.apiAuth}/admin/users`);
    }
    forkJoin(calls).subscribe({
      next: (res: any) => {
        this.projects.set(res.projects);
        this.notifications.set(res.notifications);
        if (res.users) this.users.set(res.users);
        this.loading.set(false);
        this.lastRefresh.set(new Date());
      },
      error: () => this.loading.set(false),
    });
  }

  eventIcon(type: string): string {
    const m: Record<string,string> = {PIPELINE_SUCCESS:'✓',PIPELINE_FAILED:'✕',DEPLOYMENT_FAILED:'⊗',SECURITY_BLOCKED:'🛡',SECURITY_WARNING:'⚠',UPDATE_PROJECT:'↻'};
    return m[type] ?? '•';
  }
  eventColor(type: string): string {
    if (type==='PIPELINE_SUCCESS') return 'green';
    if (type?.startsWith('SECURITY')||type==='PIPELINE_FAILED'||type==='DEPLOYMENT_FAILED') return 'red';
    return 'orange';
  }
  eventLabel(type: string): string {
    const m: Record<string,string> = {PIPELINE_SUCCESS:'Pipeline réussi',PIPELINE_FAILED:'Pipeline échoué',DEPLOYMENT_FAILED:'Déploiement échoué',SECURITY_BLOCKED:'Alerte sécurité',SECURITY_WARNING:'Avertissement',UPDATE_PROJECT:'Projet mis à jour'};
    return m[type] ?? type;
  }
  timeAgo(dateStr: string): string {
    const m = Math.floor((Date.now()-new Date(dateStr).getTime())/60000);
    if (m<1) return 'à l\'instant'; if (m<60) return `il y a ${m}m`;
    const h=Math.floor(m/60); if (h<24) return `il y a ${h}h`;
    return `il y a ${Math.floor(h/24)}j`;
  }
  roleColor(role: string): string {
    const m: Record<string,string> = {ADMIN:'#7c3aed',DEV:'#2563eb',DEVOPS:'#059669',AUDITOR:'#d97706'};
    return m[role] ?? '#6b7280';
  }
  roleIcon2(role: string): string {
    const m: Record<string,string> = {ADMIN:'👑',DEV:'💻',DEVOPS:'🚀',AUDITOR:'🔍'};
    return m[role] ?? '👤';
  }
}
