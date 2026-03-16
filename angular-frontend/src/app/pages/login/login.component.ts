import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService, LoginRequest } from '../../core/services/auth.service';
import { NavbarComponent } from '../../shared/components/navbar/navbar.component';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [FormsModule, RouterLink, NavbarComponent],
  template: `
  <div class="min-h-screen flex flex-col" style="background:#070b14; position:relative; overflow:hidden;">
    <app-navbar />

    <!-- Background orbs -->
    <div class="fixed w-[450px] h-[450px] rounded-full pointer-events-none -top-24 -right-24 blur-[90px] animate-orb-1"
         style="background:radial-gradient(circle, rgba(56,189,248,0.12) 0%, transparent 70%)"></div>
    <div class="fixed w-[350px] h-[350px] rounded-full pointer-events-none -bottom-20 -left-20 blur-[90px] animate-orb-2"
         style="background:radial-gradient(circle, rgba(129,140,248,0.1) 0%, transparent 70%)"></div>

    <!-- Card -->
    <div class="flex-1 flex items-center justify-center px-6 py-12 relative z-10">
      <div class="w-full max-w-md glass-card p-10" style="background:rgba(13,18,32,0.8)">

        <div class="text-5xl text-center mb-4" style="filter:drop-shadow(0 0 12px rgba(56,189,248,0.5))">⚽</div>
        <h1 class="text-2xl text-center mb-1">Welcome back</h1>
        <p class="text-slate-400 text-sm text-center mb-7">Sign in to manage your squad.</p>

        <!-- Error -->
        @if (error) {
          <div class="mb-5 px-4 py-3 rounded-lg text-red-400 text-sm text-center"
               style="background:rgba(248,113,113,0.1); border:1px solid rgba(248,113,113,0.3)">
            {{ error }}
          </div>
        }

        <!-- Form -->
        <form (ngSubmit)="loginForm.form.valid && onSubmit(loginForm.form.valid)" #loginForm="ngForm" class="flex flex-col gap-4">
          <div>
            <label class="form-label">Email or Username</label>
            <input id="emailOrUsername" name="emailOrUsername" type="text"
                   class="form-input" placeholder="Enter email or username"
                   [(ngModel)]="form.emailOrUsername" required />
          </div>
          <div>
            <label class="form-label">Password</label>
            <input id="password" name="password" type="password"
                   class="form-input" placeholder="Enter your password"
                   [(ngModel)]="form.password" required />
          </div>
          <button id="login-submit" type="submit"
                  class="btn-primary w-full mt-1 py-3.5 text-base rounded-lg"
                  [disabled]="loading">
            {{ loading ? 'Signing in…' : 'Sign In →' }}
          </button>
        </form>

        <p class="text-center text-sm text-slate-400 mt-6">
          New here?
          <a routerLink="/register" class="text-sky-400 font-semibold hover:text-indigo-400 transition-colors no-underline">
            Create an account
          </a>
        </p>
      </div>
    </div>
  </div>
  `
})
export class LoginComponent {
  form: LoginRequest = { emailOrUsername: '', password: '' };
  error = '';
  loading = false;

  constructor(private auth: AuthService, private router: Router) { }

  onSubmit(isValid?: boolean | null): void {
    if (isValid === false) {
      this.error = 'Please fill out all fields correctly.';
      return;
    }
    this.error = '';
    this.loading = true;
    this.auth.login(this.form).subscribe({
      next: () => {
        this.loading = false;
        this.router.navigate(['/dashboard']);
      },
      error: (err) => {
        if (err.error && typeof err.error === 'object' && !err.error.message && !err.error.error) {
          // Flatten validation error map
          this.error = Object.values(err.error).join(' ');
        } else {
          this.error = err.error?.message || err.error?.error || 'Login failed. Check your credentials.';
        }
        this.loading = false;
      }
    });
  }
}
