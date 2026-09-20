import {
  Component, Input, Output, EventEmitter,
  signal, inject, OnInit, OnDestroy, computed
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Project } from '../../core/models/project.model';
import { PipelineService } from '../../core/services/pipeline.service';
import { AuthService } from '../../core/services/auth.service';
import { Pipeline, PipelineExecution } from '../../core/models/pipeline.model';

type DeployStep = 'config' | 'deploying' | 'done';

@Component({
  selector: 'app-deploy-modal',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './Deploy-modal.component.html',
  styleUrls: ['./Deploy-modal.component.scss'],
})
export class DeployModalComponent implements OnInit, OnDestroy {

  @Input() project!: Project;
  @Output() closed = new EventEmitter<void>();

  private fb          = inject(FormBuilder);
  private pipelineSvc = inject(PipelineService);
  private authSvc     = inject(AuthService);

  // ── Signals ───────────────────────────────────────────────────────────────
  step      = signal<DeployStep>('config');
  pipelines = signal<Pipeline[]>([]);
  loading   = signal(false);
  deploying = signal(false);
  error     = signal('');
  execution = signal<PipelineExecution | null>(null);

  // For inline pipeline creation when project has none
  showCreatePipeline = signal(false);
  creatingPipeline   = signal(false);

  private pollTimer: ReturnType<typeof setInterval> | null = null;
  private _result: 'success' | 'failed' | null = null;

  readonly environments = ['DEV', 'STAGING', 'PROD'];

  // Main deploy form
  form: FormGroup = this.fb.group({
    environment:    ['DEV', Validators.required],
    commitHash:     ['HEAD', [Validators.required, Validators.minLength(3)]],
    pipelineId:     ['', Validators.required],
  });

  // Pipeline creation sub-form
  pipelineForm: FormGroup = this.fb.group({
    pipelineName:    ['devsecops-pipeline', Validators.required],
    jenkinsJobName:  ['devsecops-pipeline', Validators.required],
  });

  // ── Computed ──────────────────────────────────────────────────────────────
  get repositoryUrl(): string { return this.project?.repositoryUrl ?? ''; }
  get description(): string   { return this.project?.description   ?? ''; }
  get isSuccess(): boolean    { return this._result === 'success'; }
  get isFailed(): boolean     { return this._result === 'failed'; }
  get statusLabel(): string {
    if (this.isSuccess) return 'Déploiement réussi !';
    if (this.isFailed)  return 'Déploiement échoué';
    return '';
  }

  selectedPipeline = computed(() => {
    const id = +this.form.value.pipelineId;
    return this.pipelines().find(p => p.id === id) ?? null;
  });

  // ── Lifecycle ─────────────────────────────────────────────────────────────
  ngOnInit(): void { this.loadPipelines(); }
  ngOnDestroy(): void { this.stopPolling(); }

  loadPipelines(): void {
    this.loading.set(true);
    this.error.set('');
    this.pipelineSvc.getByProject(this.project.id).subscribe({
      next: data => {
        this.pipelines.set(data);
        if (data.length > 0) {
          this.form.patchValue({ pipelineId: data[0].id });
        } else {
          // No pipeline yet — prompt user to create one
          this.showCreatePipeline.set(true);
        }
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Impossible de charger les pipelines');
        this.loading.set(false);
      },
    });
  }

  // ── Create pipeline inline ────────────────────────────────────────────────
  createPipeline(): void {
    if (this.pipelineForm.invalid) { this.pipelineForm.markAllAsTouched(); return; }
    this.creatingPipeline.set(true);
    this.error.set('');

    const req = {
      name:           this.pipelineForm.value.pipelineName.trim(),
      jenkinsJobName: this.pipelineForm.value.jenkinsJobName.trim(),
    };

    this.pipelineSvc.create(this.project.id, req).subscribe({
      next: pipeline => {
        this.pipelines.update(list => [...list, pipeline]);
        this.form.patchValue({ pipelineId: pipeline.id });
        this.showCreatePipeline.set(false);
        this.creatingPipeline.set(false);
      },
      error: e => {
        this.error.set(e?.error?.message ?? 'Erreur création pipeline');
        this.creatingPipeline.set(false);
      },
    });
  }

  // ── Deploy ────────────────────────────────────────────────────────────────
  close(): void { this.stopPolling(); this.closed.emit(); }

  confirmDeploy(): void {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }

    const pipelineId = +this.form.value.pipelineId;
    if (!pipelineId) {
      this.error.set('Veuillez sélectionner un pipeline');
      return;
    }

    this.error.set('');
    this.deploying.set(true);
    this.step.set('deploying');

    const commitHash = this.form.value.commitHash as string;
    const userId     = this.authSvc.username || 'admin';

    this.pipelineSvc.trigger(pipelineId, { commitHash, userId }).subscribe({
      next: exec => {
        this.execution.set(exec);
        this.deploying.set(false);
        this.startPolling(exec.id);
      },
      error: e => {
        this.deploying.set(false);
        this._result = 'failed';
        this.error.set(e?.error?.message ?? 'Erreur lors du déploiement');
        this.step.set('done');
      },
    });
  }

  // ── Polling ───────────────────────────────────────────────────────────────
  private startPolling(execId: number): void {
    this.pollTimer = setInterval(() => {
      this.pipelineSvc.getExecution(execId).subscribe({
        next: exec => {
          this.execution.set(exec);
          if (exec.status === 'SUCCESS') {
            this.stopPolling();
            this._result = 'success';
            this.step.set('done');
          } else if (exec.status === 'FAILED' || exec.status === 'CANCELLED') {
            this.stopPolling();
            this._result = 'failed';
            this.error.set(`Le déploiement a échoué (${exec.status})`);
            this.step.set('done');
          }
        },
      });
    }, 4000);
  }

  private stopPolling(): void {
    if (this.pollTimer) { clearInterval(this.pollTimer); this.pollTimer = null; }
  }
}
