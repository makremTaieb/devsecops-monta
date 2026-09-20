import { Component, OnInit, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ProjectService }  from '../../core/services/project.service';
import { SecurityService } from '../../core/services/security.service';
import { Project }         from '../../core/models/project.model';
import { SecurityScan, SeverityLevel, Vulnerability } from '../../core/models/security.model';

@Component({
  selector:    'app-security',
  standalone:  true,
  imports:     [CommonModule, FormsModule],
  templateUrl: './security.component.html',
  styleUrls:   ['./security.component.scss'],
})
export class SecurityComponent implements OnInit {

  projects     = signal<Project[]>([]);
  scans        = signal<SecurityScan[]>([]);
  selectedId   = signal<number | null>(null);
  loading      = signal(false);
  activeScanId = signal<number | null>(null);
  filterSev    = signal<string>('ALL');
  filterType   = signal<string>('ALL');
  searchTerm   = signal<string>('');

  // ── Analytics ──────────────────────────────────────────────────
  readonly allVulns = computed(() =>
    this.scans().flatMap(s => s.vulnerabilities ?? [])
  );

  readonly filteredVulns = computed(() => {
    let v = this.allVulns();
    if (this.filterSev()  !== 'ALL') v = v.filter(x => x.severity === this.filterSev());
    if (this.filterType() !== 'ALL') v = v.filter(x => x.type     === this.filterType());
    const q = this.searchTerm().toLowerCase();
    if (q) v = v.filter(x =>
      x.description?.toLowerCase().includes(q) ||
      x.cve?.toLowerCase().includes(q) ||
      x.filePath?.toLowerCase().includes(q)
    );
    return v;
  });

  readonly sevCounts = computed(() => {
    const v = this.allVulns();
    return {
      CRITICAL: v.filter(x => x.severity === 'CRITICAL').length,
      HIGH:     v.filter(x => x.severity === 'HIGH').length,
      MEDIUM:   v.filter(x => x.severity === 'MEDIUM').length,
      LOW:      v.filter(x => x.severity === 'LOW').length,
    };
  });

  readonly typeCounts = computed(() => {
    const v = this.allVulns();
    return {
      SAST:   v.filter(x => x.type === 'SAST').length,
      SCA:    v.filter(x => x.type === 'SCA').length,
      SECRET: v.filter(x => x.type === 'SECRET').length,
    };
  });

  readonly avgScore = computed(() => {
    const s = this.scans();
    if (!s.length) return 0;
    const scores = s.map(x => x.securityScore ?? x.score ?? 0);
    return Math.round(scores.reduce((a, b) => a + b, 0) / scores.length);
  });

  readonly blockedCount = computed(() => this.scans().filter(s => s.blocked).length);

  // Donut segments for severity
  readonly sevDonut = computed(() => {
    const c = this.sevCounts();
    const total = c.CRITICAL + c.HIGH + c.MEDIUM + c.LOW;
    if (total === 0) return [];
    const r = 45;
    const circ = 2 * Math.PI * r;
    const items = [
      { label: 'Critique', value: c.CRITICAL, color: '#dc2626' },
      { label: 'Haute',    value: c.HIGH,     color: '#f97316' },
      { label: 'Moyenne',  value: c.MEDIUM,   color: '#eab308' },
      { label: 'Faible',   value: c.LOW,      color: '#22c55e' },
    ].filter(s => s.value > 0);
    let offset = 0;
    return items.map(seg => {
      const dash = (seg.value / total) * circ;
      const r2 = { ...seg, dash, gap: circ - dash, offset };
      offset += dash;
      return r2;
    });
  });

  // Score bar for each scan
  scoreBarWidth(scan: SecurityScan): number {
    return Math.max(0, Math.min(100, scan.securityScore ?? scan.score ?? 0));
  }

  constructor(
    private projSvc: ProjectService,
    private secSvc:  SecurityService,
  ) {}

  ngOnInit(): void {
    this.projSvc.getAll().subscribe({ next: p => this.projects.set(p) });
  }

  onProjectChange(ev: Event): void {
    const id = +(ev.target as HTMLSelectElement).value;
    this.selectedId.set(id || null);
    this.scans.set([]);
    this.activeScanId.set(null);
    if (id) {
      this.loading.set(true);
      this.secSvc.getByProject(id).subscribe({
        next:  s => { this.scans.set(s); this.loading.set(false); },
        error: () => this.loading.set(false),
      });
    }
  }

  toggleScan(id: number): void {
    this.activeScanId.update(v => v === id ? null : id);
  }

  countBySeverity(scan: SecurityScan, sev: SeverityLevel): number {
    return scan.vulnerabilities?.filter(v => v.severity === sev).length ?? 0;
  }

  scoreLabel(score: number): string {
    if (score >= 80) return 'Excellent';
    if (score >= 60) return 'Acceptable';
    if (score >= 40) return 'Risqué';
    return 'Critique';
  }

  scoreClass(score: number): string {
    if (score >= 70) return 'score-good';
    if (score >= 40) return 'score-warn';
    return 'score-bad';
  }

  sevClass(sev: string): string { return 'sev-' + sev.toLowerCase(); }
  typeClass(t: string): string  { return 'type-' + t.toLowerCase(); }

  setSevFilter(v: string)    { this.filterSev.set(v); }
  setTypeFilter(v: string)   { this.filterType.set(v); }
  setSearch(ev: Event)       { this.searchTerm.set((ev.target as HTMLInputElement).value); }

  scoreTrendPoints(): string {
    const s = this.scans().slice().reverse();
    const len = Math.max(1, s.length - 1);
    return s.map((scan, i) => {
      const x = i * (200 / len);
      const y = 60 - ((scan.securityScore ?? scan.score ?? 0) * 0.6);
      return x + ',' + y;
    }).join(' ');
  }

  readonly Math = Math;


  /** Safe score accessor — backend may return securityScore or score */
  scoreOf(scan: SecurityScan): number {
    return scan.securityScore ?? scan.score ?? 0;
  }

}
