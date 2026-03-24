import { Component, OnInit } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { ApiService } from '../../../core/services/api.service';
import { Router } from '@angular/router';

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
export class NavbarComponent implements OnInit {
  private readonly backendBase = 'http://localhost:8080';
  menuOpen = false;
  teamComplete = false;

  constructor(private auth: AuthService, private api: ApiService, private router: Router) { }

  ngOnInit() {
    if (this.isAuth) {
      this.api.getMyTeam().subscribe({
        next: (team) => { this.teamComplete = team.playerCount >= 15; },
        error: () => { this.teamComplete = false; }
      });
    }
  }

  get isAuth(): boolean { return this.auth.isAuthenticated(); }
  get showPointsAndSubs(): boolean {
    return this.teamComplete || this.isOnPointsOrSubstitutionsRoute();
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

  private isOnPointsOrSubstitutionsRoute(): boolean {
    const path = this.router.url || '';
    return path.startsWith('/points') || path.startsWith('/substitutions');
  }

  logout(): void { this.auth.logout(); }
}
