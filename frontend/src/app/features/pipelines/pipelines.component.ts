import { Component, signal, OnInit, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormControl, ReactiveFormsModule, Validators } from '@angular/forms';
import { PipelineService } from '../../core/services/pipeline.service';
import { ProjectService }  from '../../core/services/project.service';
import { Pipeline, PipelineExecution, PipelineStatus } from '../../core/models/pipeline.model';

@Component({
  selector:    'app-pipelines',
  standalone:  true,
  imports:     [CommonModule, ReactiveFormsModule],
  templateUrl: './pipelines.component.html',
  styleUrls:   ['./pipelines.component.scss'],
})
export class PipelinesComponent implements OnInit {

  projects         = signal<any[]>([]);
  pipelines        = signal<Pipeline[]>([]);
  executions       = signal<PipelineExecution[]>([]);
  selectedProject  = signal<any | null>(null);
  selectedPipeline = signal<Pipeline | null>(null);
  expandedExec     = signal<number | null>(null);
  triggering       = signal(false);
  triggerError     = signal('');
  loadingExecs     = signal(false);

  commitCtrl = new FormControl('HEAD', [Validators.required, Validators.minLength(3)]);

  // ── Analytics computed ─────────────────────────────────────────
  readonly stats = computed(() => {
    const execs = this.executions();
    const total    = execs.length;
    const success  = execs.filter(e => e.status === 'SUCCESS').length;
    const failed   = execs.filter(e => e.status === 'FAILED').length;
    const running  = execs.filter(e => e.status === 'RUNNING').length;
    const rate     = total === 0 ? 0 : Math.round((success / total) * 100);
    const durations = execs
      .filter(e => e.startTime && e.endTime)
      .map(e => (new Date(e.endTime).getTime() - new Date(e.startTime).getTime()) / 1000);
    const avgDuration = durations.length
      ? Math.round(durations.reduce((a, b) => a + b, 0) / durations.length)
      : 0;
    const minDuration = durations.length ? Math.min(...durations) : 0;
    const maxDuration = durations.length ? Math.max(...durations) : 0;
    return { total, success, failed, running, rate, avgDuration, minDuration, maxDuration };
  });

  // ── Timeline bars for SVG ──────────────────────────────────────
  readonly timelineBars = computed(() => {
    const execs = [...this.executions()].slice(0, 20).reverse();
    const maxD  = Math.max(1, ...execs
      .filter(e => e.startTime && e.endTime)
      .map(e => (new Date(e.endTime).getTime() - new Date(e.startTime).getTime()) / 1000)
    );
    return execs.map((e, i) => {
      const dur = e.startTime && e.endTime
        ? (new Date(e.endTime).getTime() - new Date(e.startTime).getTime()) / 1000
        : 0;
      return {
        exec: e,
        i,
        width: dur === 0 ? 5 : Math.max(5, Math.round((dur / maxD) * 100)),
        color: e.status === 'SUCCESS' ? '#16a34a'
             : e.status === 'FAILED'  ? '#dc2626'
             : e.status === 'RUNNING' ? '#3b82f6'
             : '#9ca3af',
        dur,
      };
    });
  });

  // ── Success trend (last 10 executions) ────────────────────────
  readonly trendPoints = computed(() => {
    const execs = [...this.executions()].slice(0, 10).reverse();
    return execs.map((e, i) => ({
      x: i * (100 / Math.max(1, execs.length - 1)),
      y: e.status === 'SUCCESS' ? 10 : e.status === 'FAILED' ? 90 : 50,
      status: e.status,
    }));
  });

  readonly trendPolyline = computed(() =>
    this.trendPoints().map(p => `${p.x},${p.y}`).join(' ')
  );

  constructor(
    private pipelineService: PipelineService,
    private projectService:  ProjectService,
  ) {}

  ngOnInit(): void {
    this.projectService.getAll().subscribe({ next: p => this.projects.set(p) });
  }

  onProjectChange(event: any): void {
    const id = +event.target.value;
    if (!id) return;
    this.selectedProject.set(id);
    this.selectedPipeline.set(null);
    this.executions.set([]);
    this.pipelineService.getByProject(id).subscribe({ next: r => this.pipelines.set(r) });
  }

  selectPipeline(p: Pipeline): void {
    this.selectedPipeline.set(p);
    this.loadingExecs.set(true);
    this.pipelineService.getExecutions(p.id).subscribe({
      next:  r => { this.executions.set(r); this.loadingExecs.set(false); },
      error: () => this.loadingExecs.set(false),
    });
  }

  toggleExec(id: number): void {
    this.expandedExec.update(v => v === id ? null : id);
  }

  trigger(): void {
    if (!this.selectedPipeline() || this.commitCtrl.invalid) return;
    this.triggering.set(true);
    this.triggerError.set('');
    this.pipelineService.trigger(this.selectedPipeline()!.id, {
      commitHash: this.commitCtrl.value!,
      userId:     'admin',
    }).subscribe({
      next: () => {
        this.triggering.set(false);
        this.commitCtrl.reset('HEAD');
        this.selectPipeline(this.selectedPipeline()!);
      },
      error: () => { this.triggerError.set('Erreur lors du déclenchement'); this.triggering.set(false); },
    });
  }

  statusClass(s: PipelineStatus): string {
    return ({ SUCCESS:'st-success', FAILED:'st-failed', RUNNING:'st-running',
              PENDING:'st-pending', CANCELLED:'st-cancelled', CREATED:'st-created' })[s] ?? '';
  }

  statusLabel(s: PipelineStatus): string {
    return ({ SUCCESS:'Succès', FAILED:'Échoué', RUNNING:'En cours',
              PENDING:'En attente', CANCELLED:'Annulé', CREATED:'Créé' })[s] ?? s;
  }

  duration(start: string, end: string): string {
    if (!start || !end) return '—';
    const s = Math.floor((new Date(end).getTime() - new Date(start).getTime()) / 1000);
    if (s < 60)  return `${s}s`;
    if (s < 3600) return `${Math.floor(s/60)}m ${s%60}s`;
    return `${Math.floor(s/3600)}h ${Math.floor((s%3600)/60)}m`;
  }

  fmtDuration(s: number): string {
    if (s < 60)   return `${s}s`;
    if (s < 3600) return `${Math.floor(s/60)}m ${Math.round(s%60)}s`;
    return `${Math.floor(s/3600)}h ${Math.floor((s%3600)/60)}m`;
  }

  stageIcon(status: string): string {
    return { SUCCESS:'✓', FAILED:'✕', RUNNING:'⟳', PENDING:'○' }[status] ?? '•';
  }
}
