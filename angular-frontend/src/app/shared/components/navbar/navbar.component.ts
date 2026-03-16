import { Component, OnInit } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { ApiService } from '../../../core/services/api.service';

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
  menuOpen = false;
  teamComplete = false;

  constructor(private auth: AuthService, private api: ApiService) { }

  ngOnInit() {
    if (this.isAuth) {
      this.api.getMyTeam().subscribe({
        next: (team) => { this.teamComplete = team.playerCount >= 15; },
        error: () => { this.teamComplete = false; }
      });
    }
  }

  get isAuth(): boolean { return this.auth.isAuthenticated(); }
  get username(): string { return this.auth.getUser()?.username ?? 'U'; }
  logout(): void { this.auth.logout(); }
}
