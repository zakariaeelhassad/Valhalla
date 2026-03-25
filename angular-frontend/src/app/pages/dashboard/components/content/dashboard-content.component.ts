import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import {
  DashboardStats,
  Deadline,
  LeaderboardEntry,
  PlayerAvailability,
  PlayerSummary,
  PlayerTransferStat,
  TeamResponse,
  TransferWindowStatus
} from '../../../../core/models';
import { AuthService, UserResponse } from '../../../../core/services/auth.service';
import { DashboardStateService } from '../../../../core/services/dashboard-state.service';
import { NavbarComponent } from '../../../../shared/components/navbar/navbar.component';
import { getTeamJersey, getTeamLogo } from '../../../../shared/utils/team-visuals';
import { ApiService } from '../../../../core/services/api.service';
import { Subscription, interval, of } from 'rxjs';
import { catchError, startWith, switchMap } from 'rxjs/operators';

const POS_META: Record<string, { label: string; color: string; borderClass: string }> = {
  GK: { label: 'Goalkeeper', color: '#fbbf24', borderClass: 'border-amber-400' },
  DEF: { label: 'Defenders', color: '#38bdf8', borderClass: 'border-sky-400' },
  MID: { label: 'Midfielders', color: '#4ade80', borderClass: 'border-emerald-400' },
  FWD: { label: 'Forwards', color: '#f87171', borderClass: 'border-red-400' },
};

interface CountdownTime {
  days: number;
  hours: number;
  minutes: number;
  seconds: number;
  totalSeconds: number;
}

interface TeamComposition {
  gk: number;
  def: number;
  mid: number;
  fwd: number;
}

@Component({
  selector: 'app-dashboard-content',
  standalone: true,
  imports: [CommonModule, NavbarComponent],
  templateUrl: './dashboard-content.component.html',
  styles: [`
    .custom-scrollbar::-webkit-scrollbar { width: 6px; height: 6px; }
    .custom-scrollbar::-webkit-scrollbar-track { background: rgba(255,255,255,0.02); border-radius: 4px; }
    .custom-scrollbar::-webkit-scrollbar-thumb { background: rgba(255,255,255,0.1); border-radius: 4px; }
    .custom-scrollbar::-webkit-scrollbar-thumb:hover { background: rgba(255,255,255,0.2); }

    .countdown-digit {
      text-align: center;
      padding: 0.6rem 0.45rem;
      background: rgba(255,255,255,0.05);
      border-radius: 0.375rem;
      border: 1px solid rgba(255,255,255,0.1);
    }

    .countdown-digit.low-time {
      background: rgba(239,68,68,0.1);
      border-color: rgba(239,68,68,0.3);
    }

    .countdown-label {
      font-size: 0.625rem;
      font-weight: bold;
      text-transform: uppercase;
      letter-spacing: 0.05em;
      color: rgb(100,116,139);
      margin-top: 0.25rem;
    }

    .team-position-badge {
      display: inline-flex;
      align-items: center;
      justify-content: center;
      padding: 0.375rem 0.75rem;
      border-radius: 9999px;
      font-size: 0.75rem;
      font-weight: 600;
      margin-right: 0.375rem;
      margin-bottom: 0.375rem;
    }

    .stat-mini {
      border: 1px solid rgba(148,163,184,0.2);
      background: rgba(15,23,42,0.45);
      border-radius: 0.9rem;
      padding: 0.95rem;
    }
  `]
})
export class DashboardContentComponent implements OnInit, OnDestroy {
  user: UserResponse | null = null;
  team: TeamResponse | null = null;
  dashboardStats: DashboardStats | null = null;
  transferWindowStatus: TransferWindowStatus | null = null;
  posMeta = POS_META;
  readonly totalPlayers = 13002458;
  private readonly backendBase = 'http://localhost:8081';

  countdownTime: CountdownTime = { days: 0, hours: 0, minutes: 0, seconds: 0, totalSeconds: 0 };
  isLowTime = false;
  teamComposition: TeamComposition = { gk: 0, def: 0, mid: 0, fwd: 0 };
  squadValue = 0;
  topOverallUserName = 'No data';
  topOverallUserPoints = 0;

  private stateSubscription?: Subscription;
  private countdownSubscription?: Subscription;
  private autoRefreshSubscription?: Subscription;
  private transferWindowSubscription?: Subscription;
  private leaderboardSubscription?: Subscription;
  private serverNowAtFetchMs: number | null = null;
  private serverNowFetchedAtClientMs: number | null = null;

  get nextDeadline(): Deadline | undefined {
    return this.dashboardStats?.deadlines?.find((d: Deadline) => d.isNext);
  }

  get upcomingDeadlines(): Deadline[] {
    return this.dashboardStats?.deadlines?.filter((d: Deadline) => !d.isNext) || [];
  }

  get effectiveNextDeadline(): { gameweek: number; deadlineTime: string } | null {
    if (this.transferWindowStatus?.nextDeadline) {
      const fallbackGw = (this.dashboardStats?.gameState?.currentGameweek ?? 0) + 1;
      return {
        gameweek: this.transferWindowStatus.nextGameweek ?? fallbackGw,
        deadlineTime: this.transferWindowStatus.nextDeadline
      };
    }

    if (this.nextDeadline) {
      return {
        gameweek: this.nextDeadline.gameweek,
        deadlineTime: this.nextDeadline.deadlineTime
      };
    }

    return null;
  }

  get topTransfersIn(): PlayerTransferStat[] {
    return this.dashboardStats?.topTransfersIn || [];
  }

  get topTransfersOut(): PlayerTransferStat[] {
    return this.dashboardStats?.topTransfersOut || [];
  }

  get potw(): PlayerSummary[] {
    return this.dashboardStats?.potw || [];
  }

  get totw(): PlayerSummary[] {
    return this.dashboardStats?.totw || [];
  }

  get availability(): PlayerAvailability[] {
    return this.dashboardStats?.availability || [];
  }

  get playerCount(): number {
    return this.team?.playerCount || this.team?.players?.length || 0;
  }

  get inBank(): number {
    return this.team?.remainingBudget ?? 0;
  }

  get currentGameweek(): number {
    return this.dashboardStats?.gameState?.currentGameweek ?? 0;
  }

  get topPreviousGameweekPlayer(): PlayerSummary | null {
    const pool = this.potw.length > 0 ? this.potw : this.totw;
    if (!pool.length) {
      return null;
    }
    return [...pool].sort((a, b) => (b.totalPoints || 0) - (a.totalPoints || 0))[0] || null;
  }

  constructor(
    public auth: AuthService,
    public state: DashboardStateService,
    private router: Router,
    private api: ApiService
  ) { }

  ngOnInit(): void {
    if (!this.auth.isAuthenticated()) {
      this.router.navigate(['/login']);
      return;
    }

    this.user = this.auth.getUser();
    this.state.loadDashboardData();

    this.stateSubscription = new Subscription();

    this.stateSubscription.add(
      this.state.dashboardStats$.subscribe(stats => {
        this.dashboardStats = stats;
      })
    );

    this.stateSubscription.add(
      this.state.team$.subscribe(team => {
        this.team = team;
        this.updateTeamComposition(team);
        this.updateSquadValue(team);
      })
    );

    this.startCountdownTimer();

    this.leaderboardSubscription = interval(60000)
      .pipe(
        startWith(0),
        switchMap(() => this.api.getLeaderboard().pipe(catchError(() => of([] as LeaderboardEntry[]))))
      )
      .subscribe(rows => {
        const top = rows?.[0];
        this.topOverallUserName = top?.username || 'No data';
        this.topOverallUserPoints = top?.totalPoints ?? 0;
      });

    this.transferWindowSubscription = interval(30000)
      .pipe(
        startWith(0),
        switchMap(() => this.api.getTransferWindowStatus().pipe(catchError(() => of(null))))
      )
      .subscribe(status => {
        this.transferWindowStatus = status;
        const parsedServerNow = this.parseBackendDateToMs(status?.currentDate || null);
        if (parsedServerNow !== null) {
          this.serverNowAtFetchMs = parsedServerNow;
          this.serverNowFetchedAtClientMs = Date.now();
        }
        this.updateCountdown();
      });

    this.autoRefreshSubscription = interval(60000)
      .pipe(startWith(0))
      .subscribe(() => {
        this.state.loadDashboardData();
      });
  }

  ngOnDestroy(): void {
    this.stateSubscription?.unsubscribe();
    this.countdownSubscription?.unsubscribe();
    this.autoRefreshSubscription?.unsubscribe();
    this.transferWindowSubscription?.unsubscribe();
    this.leaderboardSubscription?.unsubscribe();
  }

  private startCountdownTimer(): void {
    this.countdownSubscription = interval(1000)
      .pipe(startWith(0))
      .subscribe(() => {
        this.updateCountdown();
      });
  }

  private updateCountdown(): void {
    const nextDeadline = this.effectiveNextDeadline;
    if (!nextDeadline) {
      this.countdownTime = { days: 0, hours: 0, minutes: 0, seconds: 0, totalSeconds: 0 };
      this.isLowTime = false;
      return;
    }

    const deadlineDate = this.parseBackendDateToMs(nextDeadline.deadlineTime);
    if (deadlineDate === null) {
      this.countdownTime = { days: 0, hours: 0, minutes: 0, seconds: 0, totalSeconds: 0 };
      this.isLowTime = false;
      return;
    }

    const now = this.getReferenceNowMs();
    const diff = deadlineDate - now;

    if (diff <= 0) {
      this.countdownTime = { days: 0, hours: 0, minutes: 0, seconds: 0, totalSeconds: 0 };
      this.isLowTime = true;
    } else {
      const totalSeconds = Math.floor(diff / 1000);
      this.countdownTime = {
        days: Math.floor(totalSeconds / (24 * 3600)),
        hours: Math.floor((totalSeconds % (24 * 3600)) / 3600),
        minutes: Math.floor((totalSeconds % 3600) / 60),
        seconds: totalSeconds % 60,
        totalSeconds
      };
      // Mark as low time if less than 6 hours remaining
      this.isLowTime = totalSeconds < 6 * 3600;
    }
  }

  private getReferenceNowMs(): number {
    if (this.serverNowAtFetchMs !== null && this.serverNowFetchedAtClientMs !== null) {
      const elapsedClientMs = Date.now() - this.serverNowFetchedAtClientMs;
      return this.serverNowAtFetchMs + Math.max(0, elapsedClientMs);
    }
    return Date.now();
  }

  private parseBackendDateToMs(value: string | null | undefined): number | null {
    if (!value) {
      return null;
    }

    const raw = value.trim();
    if (!raw) {
      return null;
    }

    // Backend sends LocalDateTime in UTC without timezone suffix.
    const hasTimezone = /[zZ]|[+\-]\d{2}:\d{2}$/.test(raw);
    const normalized = hasTimezone ? raw : `${raw}Z`;

    const asUtc = Date.parse(normalized);
    if (!Number.isNaN(asUtc)) {
      return asUtc;
    }

    const fallback = Date.parse(raw);
    return Number.isNaN(fallback) ? null : fallback;
  }

  private updateTeamComposition(team: TeamResponse | null): void {
    const players = team?.players || [];
    this.teamComposition = {
      gk: players.filter((p: any) => p.position === 'GK').length,
      def: players.filter((p: any) => p.position === 'DEF').length,
      mid: players.filter((p: any) => p.position === 'MID').length,
      fwd: players.filter((p: any) => p.position === 'FWD').length
    };
  }

  private updateSquadValue(team: TeamResponse | null): void {
    const players = team?.players || [];
    this.squadValue = players.reduce((sum: number, p: any) => sum + (p.price || 0), 0);
  }

  go(path: string): void {
    this.router.navigate([path]);
  }

  refresh(): void {
    this.state.refresh();
  }

  logout(): void {
    this.auth.logout();
  }

  formatCountdownValue(value: number): string {
    return String(value).padStart(2, '0');
  }

  getProfileImageSrc(): string | null {
    const value = this.user?.profileImage ?? null;
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

  getTeamImageSrc(): string | null {
    const value = this.team?.teamImage ?? null;
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

  getClubLogo(team: string | null | undefined): string {
    return getTeamLogo(team || '');
  }

  getJersey(team: string | null | undefined): string {
    return getTeamJersey(team || '');
  }
}
