import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService, RegisterRequest } from '../../core/services/auth.service';
import { NavbarComponent } from '../../shared/components/navbar/navbar.component';

@Component({
  selector: 'app-register',
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

    <div class="flex-1 flex items-center justify-center px-6 py-12 relative z-10">
      <div class="w-full max-w-md glass-card p-10" style="background:rgba(13,18,32,0.8)">

        <div class="text-5xl text-center mb-4" style="filter:drop-shadow(0 0 12px rgba(56,189,248,0.5))">⚽</div>
        <h1 class="text-2xl text-center mb-1">Create Account</h1>
        <p class="text-slate-400 text-sm text-center mb-7">Join thousands of managers. It's free.</p>

        @if (error) {
          <div class="mb-5 px-4 py-3 rounded-lg text-red-400 text-sm text-center"
               style="background:rgba(248,113,113,0.1); border:1px solid rgba(248,113,113,0.3)">
            {{ error }}
          </div>
        }

        @if (success) {
          <div class="mb-5 px-4 py-3 rounded-lg text-emerald-400 text-sm text-center"
               style="background:rgba(74,222,128,0.1); border:1px solid rgba(74,222,128,0.3)">
            Account created! Redirecting to dashboard…
          </div>
        }

        <form (ngSubmit)="regForm.form.valid && onSubmit(regForm.form.valid)" #regForm="ngForm" class="flex flex-col gap-4">
          <div>
            <label class="form-label">Username</label>
            <input id="username" name="username" type="text"
                   class="form-input" placeholder="Choose a username"
                   [(ngModel)]="form.username" required minlength="3" />
          </div>
          <div>
            <label class="form-label">Email</label>
            <input id="email" name="email" type="email"
                   class="form-input" placeholder="your@email.com"
                   [(ngModel)]="form.email" required />
          </div>
          <div>
            <label class="form-label">Password</label>
            <input id="reg-password" name="password" type="password"
                   class="form-input" placeholder="Min. 6 characters"
                   [(ngModel)]="form.password" required minlength="6" />
          </div>
          <button id="register-submit" type="submit"
                  class="btn-primary w-full mt-1 py-3.5 text-base rounded-lg"
                  [disabled]="loading">
            {{ loading ? 'Creating account…' : 'Create Account →' }}
          </button>
        </form>

        <p class="text-center text-sm text-slate-400 mt-6">
          Already a manager?
          <a routerLink="/login" class="text-sky-400 font-semibold hover:text-indigo-400 transition-colors no-underline">
            Sign in
          </a>
        </p>
      </div>
    </div>
  </div>
  `
})
export class RegisterComponent {
  form: RegisterRequest = { username: '', email: '', password: '' };
  error = '';
  success = false;
  loading = false;

  constructor(private auth: AuthService, private router: Router) { }

  onSubmit(isValid?: boolean | null): void {
    if (isValid === false) {
      this.error = 'Please fill out all fields correctly.';
      return;
    }
    this.error = '';
    this.loading = true;
    this.auth.register(this.form).subscribe({
      next: () => {
        this.success = true;
        this.loading = false;
        setTimeout(() => this.router.navigate(['/dashboard']), 1200);
      },
      error: (err) => {
        if (err.error && typeof err.error === 'object' && !err.error.message && !err.error.error) {
          // Flatten validation error map
          this.error = Object.values(err.error).join(' ');
        } else {
          this.error = err.error?.message || err.error?.error || 'Registration failed. Please try again.';
        }
        this.loading = false;
      }
    });
  }
}
