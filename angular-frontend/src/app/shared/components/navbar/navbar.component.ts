import { Component } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [RouterLink, RouterLinkActive],
  templateUrl: './navbar.component.html',
  styles: [`
    .mob-link {
      display: block; padding: 0.75rem 1rem; color: rgb(148 163 184);
      font-size: 0.875rem; font-weight: 500; border-radius: 0.5rem; transition: all 0.2s;
    }
    .mob-link:hover { color: rgb(241 245 249); background: rgba(255,255,255,0.05); }
  `]
})
export class NavbarComponent {
  private readonly backendBase = 'http://localhost:8081';
  menuOpen = false;

  constructor(private auth: AuthService) { }

  get isAuth(): boolean { return this.auth.isAuthenticated(); }
  get showPointsAndSubs(): boolean {
    return this.isAuth;
  }
  get username(): string { return this.auth.getUser()?.username ?? 'U'; }
  get profileImageSrc(): string | null {
    const value = this.auth.getUser()?.profileImage ?? null;
    if (!value) {
      return null;
    }
    if (value.startsWith('data:image/')) {
      return value;
    }
    if (value.startsWith('/')) {
      return `${this.backendBase}${value}`;
    }
    if (value.startsWith('http://') || value.startsWith('https://')) {
      return value;
    }
    return null;
  }

  logout(): void { this.auth.logout(); }
}
