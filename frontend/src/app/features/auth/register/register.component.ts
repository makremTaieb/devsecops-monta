import { Component, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import {
  ReactiveFormsModule,
  FormBuilder,
  Validators,
  FormGroup
} from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { RegisterRequest, Role } from '../../../core/models/auth.model';


@Component({
  selector: 'app-register',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './register.component.html',
  styleUrls: ['./register.component.scss'],
})
export class RegisterComponent {

  form: FormGroup;

  loading = signal(false);
  error = signal('');

  readonly roles: { value: Role; label: string; desc: string }[] = [
    { value: 'ADMIN', label: 'Administrator', desc: 'Full access' },
    { value: 'DEV', label: 'Developer', desc: 'CI/CD access' },
    { value: 'DEVOPS', label: 'DevOps Engineer', desc: 'Infrastructure' },
    { value: 'AUDITOR', label: 'Security Auditor', desc: 'Read-only security' }
  ];

  constructor(
    private fb: FormBuilder,
    private auth: AuthService,
    private router: Router
  ) {
    this.form = this.fb.group({
      username: ['', [Validators.required, Validators.minLength(3)]],
      email: ['', [Validators.required, Validators.email]],
      password: ['', [Validators.required, Validators.minLength(6)]],
      role: ['DEV' as Role, Validators.required]
    });
  }

  get f() {
    return this.form.controls;
  }

  submit(): void {

    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.loading.set(true);
    this.error.set('');

    const payload: RegisterRequest = {
      username: this.form.value.username!,
      email: this.form.value.email!,
      password: this.form.value.password!,
      role: this.form.value.role as Role
    };

    this.auth.register(payload).subscribe({
      next: () => {
        this.loading.set(false);
        this.router.navigate(['/dashboard']);
      },

      error: (err) => {
        this.error.set(err?.error?.message || 'Registration failed');
        this.loading.set(false);
      }
    });
  }
}