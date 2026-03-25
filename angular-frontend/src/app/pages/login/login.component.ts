import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService, LoginRequest } from '../../core/services/auth.service';
import { NavbarComponent } from '../../shared/components/navbar/navbar.component';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [FormsModule, RouterLink, NavbarComponent],
  templateUrl: './login.component.html'
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
