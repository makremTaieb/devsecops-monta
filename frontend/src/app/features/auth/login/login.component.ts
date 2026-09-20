import { Component, signal, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { RegisterRequest, Role } from '../../../core/models/auth.model';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.scss'],
})
export class LoginComponent implements OnInit, OnDestroy {

  form: FormGroup;
  registerForm: FormGroup;

  loading       = signal(false);
  error         = signal('');
  showPass      = signal(false);
  ready         = signal(false);
  showRegister  = signal(false);
  regLoading    = signal(false);
  regError      = signal('');
  regSuccess    = signal(false);
  showRegPass   = signal(false);

  // ALL 4 roles including ADMIN — visible by default
  readonly roles: { value: Role; label: string; desc: string; icon: string; color: string }[] = [
    {
      value: 'ADMIN',
      label: 'Administrateur',
      desc: 'Tous les privilèges — gestion des utilisateurs, projets et déploiements',
      icon: '👑',
      color: '#7c3aed',
    },
    {
      value: 'DEV',
      label: 'Développeur',
      desc: 'Création de projets, gestion du code & pipelines CI/CD',
      icon: '💻',
      color: '#2563eb',
    },
    {
      value: 'DEVOPS',
      label: 'DevOps Engineer',
      desc: 'Déploiement, infrastructure & accès à tous les projets',
      icon: '🚀',
      color: '#059669',
    },
    {
      value: 'AUDITOR',
      label: 'Auditeur Sécurité',
      desc: 'Lecture seule — consultation des logs et rapports de sécurité',
      icon: '🔍',
      color: '#d97706',
    },
  ];

  private _keyBuffer = '';
  private _keyHandler?: (e: KeyboardEvent) => void;

  constructor(
    private fb:     FormBuilder,
    private auth:   AuthService,
    private router: Router,
  ) {
    this.form = this.fb.group({
      username: ['', [Validators.required, Validators.minLength(3)]],
      password: ['', [Validators.required, Validators.minLength(6)]],
    });
    this.registerForm = this.fb.group({
      username: ['', [Validators.required, Validators.minLength(3)]],
      email:    ['', [Validators.required, Validators.email]],
      password: ['', [Validators.required, Validators.minLength(6)]],
      role:     ['DEV' as Role, Validators.required],
    });
  }

  ngOnInit(): void {
    setTimeout(() => this.ready.set(true), 60);
    if (this.auth.isLoggedIn()) this.router.navigate(['/dashboard']);
  }

  ngOnDestroy(): void {
    if (this._keyHandler) document.removeEventListener('keydown', this._keyHandler);
  }

  get f()  { return this.form.controls; }
  get rf() { return this.registerForm.controls; }

  togglePass():    void { this.showPass.update(v => !v); }
  toggleRegPass(): void { this.showRegPass.update(v => !v); }

  openRegister(): void {
    this.showRegister.set(true);
    this.regError.set('');
    this.regSuccess.set(false);
    this.registerForm.reset({ role: 'DEV' });
  }
  closeRegister(): void { this.showRegister.set(false); }

  selectRole(role: Role): void {
    this.registerForm.patchValue({ role });
  }

  submit(): void {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    this.loading.set(true);
    this.error.set('');
    this.auth.login(this.form.getRawValue()).subscribe({
      next: () => { this.loading.set(false); this.router.navigate(['/dashboard']); },
      error: (e) => {
        this.error.set(
          e.error?.message || e.error?.error ||
          (e.status === 401 ? 'Identifiants incorrects' : 'Erreur serveur — réessayez')
        );
        this.loading.set(false);
      },
    });
  }

  register(): void {
    if (this.registerForm.invalid) { this.registerForm.markAllAsTouched(); return; }
    this.regLoading.set(true);
    this.regError.set('');
    const payload: RegisterRequest = this.registerForm.getRawValue() as RegisterRequest;
    this.auth.register(payload).subscribe({
      next: () => {
        this.regLoading.set(false);
        this.regSuccess.set(true);
        setTimeout(() => { this.closeRegister(); this.router.navigate(['/dashboard']); }, 1500);
      },
      error: (e) => {
        this.regError.set(e?.error?.message || 'Erreur lors de l\'inscription');
        this.regLoading.set(false);
      },
    });
  }
}
