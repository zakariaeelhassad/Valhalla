import { Component, OnInit, OnDestroy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { interval, Subscription, switchMap, startWith, forkJoin, of } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { PointsStateService } from '../../core/services/points-state.service';
import { AuthService, UserResponse } from '../../core/services/auth.service';
import { ApiService } from '../../core/services/api.service';
import { CurrentGameweekContext, GameState, MatchResponse, PlayerSummary, TeamGameweekStats, TeamLineupResponse, TeamResponse } from '../../core/models';
import { PointsContentComponent } from './components/content/points-content.component';

type PointsPlayer = PlayerSummary & { gameweekPoints: number, starter?: boolean };

@Component({
  selector: 'app-points',
  standalone: true,
  imports: [CommonModule, PointsContentComponent],
  templateUrl: './points.component.html',
})
export class PointsComponent implements OnInit, OnDestroy {
  private readonly backendBase = 'http://localhost:8080';
  user: UserResponse | null = null;

  // Mock League Data to drive template *ngFor iteration
  // In reality, this relies on a composite backend response bound to state.pointsData$
  broadcasterLeagues = [
    { name: 'beIN SPORTS League', userRank: 1730228 }
  ];
  generalLeagues = [
    { name: 'Morocco', userRank: 114135 },
    { name: 'Gameweek 3', userRank: 210211 },
    { name: 'Overall', userRank: 8867051 },
    { name: 'Second Chance', userRank: 10541165 }
  ];

  imgP = "https://resources.premierleague.com/premierleague/photos/players/110x140/Kit_Colors_Placeholder.png";
  imgR = "data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 100 100'><rect width='100' height='100' fill='%23ef4444'/><path d='M30,100 Q50,30 70,100' fill='%23b91c1c'/><circle cx='50' cy='50' r='15' fill='%23fff'/></svg>";
  imgB = "data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 100 100'><rect width='100' height='100' fill='%233b82f6'/><path d='M30,100 Q50,30 70,100' fill='%231d4ed8'/></svg>";
  imgL = "data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 100 100'><rect width='100' height='100' fill='%237dd3fc'/><path d='M30,100 Q50,30 70,100' fill='%230284c7'/></svg>";
  imgBl = "data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 100 100'><rect width='100' height='100' fill='%231e293b'/><path d='M30,100 Q50,30 70,100' fill='%23000'/></svg>";
  imgW = "data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 100 100'><rect width='100' height='100' fill='%239f1239'/><path d='M30,100 Q50,30 70,100' fill='%237dd3fc'/></svg>";
  imgGr = "data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 100 100'><rect width='100' height='100' fill='%23059669'/><path d='M30,100 Q50,30 70,100' fill='%23047857'/></svg>";

  // Dynamic Data
  gameState: GameState | null = null;
  currentGameweek: number = 0;
  viewedGameweek: number = 0;
  gwPoints: number = 0;
  highestPoints: number = 0;
  teamResponse: TeamResponse | null = null;
  mySquad: PointsPlayer[] = [];
  pitchSquad: { gks: PointsPlayer[], defs: PointsPlayer[], mids: PointsPlayer[], fwds: PointsPlayer[] } = {
    gks: [], defs: [], mids: [], fwds: []
  };
  benchSquad: PointsPlayer[] = [];
  matches: MatchResponse[] = [];
  backendCurrentDate: string = '';
  backendCurrentDateLabel: string = '';
  dbCurrentGameweek: number = 0;

  // Polling
  private pollingSub?: Subscription;

  constructor(
    public auth: AuthService,
    public state: PointsStateService,
    private api: ApiService,
    private router: Router,
    private cdr: ChangeDetectorRef
  ) { }

  ngOnInit(): void {
    if (!this.auth.isAuthenticated()) {
      this.router.navigate(['/login']);
      return;
    }

    this.user = this.auth.getUser();

    // Load gameweek points active data (mock wrapper fallback)
    this.state.loadPointsData();

    // Setup Backend Polling (Every 10 seconds)
    this.pollingSub = interval(10000).pipe(
      startWith(0),
      switchMap(() => this.api.getCurrentGameweekContext().pipe(catchError(() => of(null)))),
      switchMap((ctx: CurrentGameweekContext | null) => {
        if (ctx?.currentDate) {
          this.backendCurrentDate = ctx.currentDate;
          this.backendCurrentDateLabel = new Date(ctx.currentDate).toLocaleString('en-GB', {
            weekday: 'short',
            day: '2-digit',
            month: 'short',
            year: 'numeric',
            hour: '2-digit',
            minute: '2-digit'
          });
        }

        if (ctx?.currentGameweek != null) {
          this.dbCurrentGameweek = ctx.currentGameweek;
          if (this.viewedGameweek === 0) {
            this.viewedGameweek = ctx.currentGameweek;
          }
        }

        const fetchGw = this.viewedGameweek || this.dbCurrentGameweek || 1;
        return forkJoin({
          ctx: of(ctx),
          gs: this.api.getGameState().pipe(catchError(() => of(null))),
          fetchGw: of(fetchGw),
          matches: (ctx?.currentGameweek === fetchGw && ctx?.matches)
            ? of(ctx.matches)
            : this.api.getMatchesByGameweek(fetchGw).pipe(catchError(() => of([]))),
          team: this.api.getMyTeam().pipe(catchError(() => of(null))),
          lineup: this.api.getTeamLineup().pipe(catchError(() => of(null))),
          stats: this.api.getGameweekStats(fetchGw).pipe(catchError(() => of(null)))
        });
      })
    ).subscribe((data) => {
      if (!data) return;

      if (data.gs) {
        this.gameState = data.gs;
        this.currentGameweek = data.gs.currentGameweek || this.dbCurrentGameweek || 1;
      } else if (this.dbCurrentGameweek > 0) {
        this.currentGameweek = this.dbCurrentGameweek;
      }

      // Update Matches only if the user hasn't navigated while request was in flight
      if (this.viewedGameweek === data.fetchGw) {
        this.matches = data.matches || [];
      }

      // Update Team
      const tr = data.team;
      const lineup = data.lineup as TeamLineupResponse | null;
      const stats = data.stats;

      if (tr && tr.players) {
        this.teamResponse = tr;
        if (lineup?.players?.length) {
          this.mySquad = lineup.players.map(p => ({
            id: p.id,
            name: p.name,
            position: p.position,
            realTeam: p.realTeam,
            price: p.price,
            totalPoints: p.totalPoints,
            gameweekPoints: 0,
            starter: p.starter
          }));
        } else {
          this.mySquad = tr.players.map(p => ({ ...p, gameweekPoints: 0 }));
        }

        if (stats) {
          this.applyGameweekStatsToSquad(stats);
        } else {
          this.gwPoints = 0;
          this.highestPoints = 0;
          this.mySquad = [];
        }

        this.rebuildPitchFromLineup();
      }
      this.cdr.detectChanges();
    });
  }

  ngOnDestroy(): void {
    if (this.pollingSub) this.pollingSub.unsubscribe();
  }

  // --- Fixtures Display Helpers ---

  get matchesByDate(): { date: string, matches: MatchResponse[] }[] {
    const grouped: { [key: string]: MatchResponse[] } = {};
    this.matches.forEach(m => {
      const d = m.kickoffTime ? m.kickoffTime.slice(0, 10) : 'TBD';
      if (!grouped[d]) grouped[d] = [];
      grouped[d].push(m);
    });
    return Object.keys(grouped).sort().map(date => {
      return {
        date: date !== 'TBD' ? new Date(date + 'T12:00:00').toLocaleDateString('en-GB', { weekday: 'long', day: 'numeric', month: 'long', year: 'numeric' }) : 'TBD',
        matches: grouped[date]
      };
    });
  }

  changeGameweek(delta: number) {
    const newGw = this.viewedGameweek + delta;
    if (newGw >= 1 && newGw <= 38) {
      this.viewedGameweek = newGw;
      // Refresh fixtures and GW player points immediately for responsive navigation.
      forkJoin({
        matches: this.api.getMatchesByGameweek(this.viewedGameweek).pipe(catchError(() => of([]))),
        stats: this.api.getGameweekStats(this.viewedGameweek).pipe(catchError(() => of(null)))
      }).subscribe(({ matches, stats }) => {
        this.matches = matches || [];
        if (stats && !this.isFutureHiddenGameweek()) {
          this.applyGameweekStatsToSquad(stats);
        } else {
          this.gwPoints = 0;
          this.highestPoints = 0;
          this.mySquad = [];
        }
        this.rebuildPitchFromLineup();
        this.cdr.detectChanges();
      });
    }
  }

  private applyGameweekStatsToSquad(stats: TeamGameweekStats): void {
    this.gwPoints = stats.teamPoints;
    this.highestPoints = stats.globalHighestPoints;

    this.mySquad = stats.players.map(p => ({
      id: p.playerId,
      name: p.name,
      position: p.position,
      realTeam: p.realTeam,
      price: p.price,
      totalPoints: p.totalPoints,
      gameweekPoints: p.points,
      starter: p.starter
    }));
  }

  private rebuildPitchFromLineup(): void {
    const starters = this.mySquad.filter(p => p.starter === true);
    const bench = this.mySquad.filter(p => p.starter !== true);

    this.pitchSquad.gks = starters.filter(p => p.position === 'GK').slice(0, 1);
    this.pitchSquad.defs = starters.filter(p => p.position === 'DEF');
    this.pitchSquad.mids = starters.filter(p => p.position === 'MID');
    this.pitchSquad.fwds = starters.filter(p => p.position === 'FWD');

    this.benchSquad = [
      ...bench.filter(p => p.position === 'GK'),
      ...bench.filter(p => p.position === 'DEF'),
      ...bench.filter(p => p.position === 'MID'),
      ...bench.filter(p => p.position === 'FWD')
    ];
  }

  isFutureHiddenGameweek(): boolean {
    const viewed = this.viewedGameweek || this.dbCurrentGameweek || 1;
    const maxVisible = this.dbCurrentGameweek || 1;
    return viewed > maxVisible;
  }

  shouldShowLineupSections(): boolean {
    return !this.isFutureHiddenGameweek() && this.mySquad.length > 0;
  }

}
