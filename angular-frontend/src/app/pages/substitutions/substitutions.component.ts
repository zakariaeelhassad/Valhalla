import { ChangeDetectorRef, Component, OnDestroy, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { catchError, finalize, forkJoin, interval, of, startWith, Subscription, switchMap } from 'rxjs';
import { ApiService, TeamLineupPlayer, TeamLineupResponse, TransferWindowStatus } from '../../core/services/api.service';
import { NavbarComponent } from '../../shared/components/navbar/navbar.component';
import { getTeamJersey, getTeamLogo } from '../../shared/utils/team-visuals';

interface LineupSlot {
  id: number;
  player: TeamLineupPlayer | null;
}

@Component({
  selector: 'app-substitutions',
  standalone: true,
  imports: [CommonModule, NavbarComponent],
  templateUrl: './substitutions.component.html',
  styles: [`
    .glass-card {
      background: rgba(30, 41, 59, 0.4);
      backdrop-filter: blur(12px);
      -webkit-backdrop-filter: blur(12px);
      border: 1px solid rgba(255, 255, 255, 0.08);
      border-radius: 1rem;
      box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1), 0 2px 4px -1px rgba(0, 0, 0, 0.06);
    }
  `]
})
export class SubstitutionsComponent implements OnInit, OnDestroy {
  private readonly backendBase = 'http://localhost:8080';
  loadingLineup = true;
  loadingGameState = true;
  saving = false;
  errorMessage: string | null = null;
  successMessage: string | null = null;

  teamName = '';
  teamImage: string | null = null;
  currentGameweek = 0;
  gameweekLocked = false;
  transferWindowStatus: TransferWindowStatus | null = null;
  nextDeadlineLabel = '';

  selectedSlotId: number | null = null;

  starterSlots: LineupSlot[] = [
    { id: 1, player: null },
    { id: 2, player: null },
    { id: 3, player: null },
    { id: 4, player: null },
    { id: 5, player: null },
    { id: 6, player: null },
    { id: 7, player: null },
    { id: 8, player: null },
    { id: 9, player: null },
    { id: 10, player: null },
    { id: 11, player: null },
  ];

  benchSlots: LineupSlot[] = [
    { id: 12, player: null },
    { id: 13, player: null },
    { id: 14, player: null },
    { id: 15, player: null },
  ];

  private savedStarterSlots: LineupSlot[] = [];
  private savedBenchSlots: LineupSlot[] = [];

  private transferWindowSub?: Subscription;

  constructor(private api: ApiService, private cdr: ChangeDetectorRef) { }

  ngOnInit(): void {
    this.transferWindowSub = interval(30000).pipe(
      startWith(0),
      switchMap(() => this.api.getTransferWindowStatus().pipe(catchError(() => of(null))))
    ).subscribe((status) => {
      if (!status) {
        this.loadingGameState = false;
        return;
      }

      this.transferWindowStatus = status;
      this.gameweekLocked = !status.transfersAllowed;
      this.currentGameweek = status.activeGameweek || status.nextGameweek || 0;
      this.nextDeadlineLabel = this.formatDeadline(status.nextDeadline);
      this.loadingGameState = false;
      this.cdr.detectChanges();
    });

    this.loadLineup();
  }

  ngOnDestroy(): void {
    this.transferWindowSub?.unsubscribe();
  }

  get starterGks(): LineupSlot[] {
    return this.starterSlots.filter(s => s.player?.position === 'GK');
  }

  get starterDefs(): LineupSlot[] {
    return this.starterSlots.filter(s => s.player?.position === 'DEF');
  }

  get starterMids(): LineupSlot[] {
    return this.starterSlots.filter(s => s.player?.position === 'MID');
  }

  get starterFwds(): LineupSlot[] {
    return this.starterSlots.filter(s => s.player?.position === 'FWD');
  }

  get hasUnsavedChanges(): boolean {
    const current = this.serializeSlots(this.starterSlots, this.benchSlots);
    const saved = this.serializeSlots(this.savedStarterSlots, this.savedBenchSlots);
    return current !== saved;
  }

  loadLineup(): void {
    this.loadingLineup = true;
    this.errorMessage = null;

    forkJoin({
      lineup: this.api.getTeamLineup(),
      team: this.api.getMyTeam().pipe(catchError(() => of(null)))
    }).pipe(
      finalize(() => {
        this.loadingLineup = false;
        this.cdr.detectChanges();
      })
    ).subscribe({
      next: ({ lineup, team }) => {
        this.applyLineup(lineup);
        this.teamImage = team?.teamImage || null;
      },
      error: (err) => {
        this.errorMessage = err.error?.message || 'Failed to load lineup.';
      }
    });
  }

  getTeamImageSrc(): string | null {
    const value = this.teamImage ?? null;
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

  selectOrSwap(slot: LineupSlot): void {
    if (this.gameweekLocked) {
      this.errorMessage = this.getLockMessage();
      return;
    }

    if (!slot.player) return;

    this.successMessage = null;
    this.errorMessage = null;

    if (this.selectedSlotId === null) {
      this.selectedSlotId = slot.id;
      return;
    }

    if (this.selectedSlotId === slot.id) {
      this.selectedSlotId = null;
      return;
    }

    const first = this.findSlotById(this.selectedSlotId);
    const second = this.findSlotById(slot.id);
    if (!first || !second) {
      this.selectedSlotId = null;
      return;
    }

    const temp = first.player;
    first.player = second.player;
    second.player = temp;
    this.selectedSlotId = null;
  }

  saveLineup(): void {
    if (this.gameweekLocked) {
      this.errorMessage = this.getLockMessage();
      return;
    }

    if (!this.hasUnsavedChanges) {
      this.errorMessage = 'No lineup changes to save.';
      return;
    }

    const formationError = this.validateFormation();
    if (formationError) {
      this.errorMessage = formationError;
      return;
    }

    this.saving = true;
    this.errorMessage = null;
    this.successMessage = null;

    const starterPlayerIds = this.starterSlots
      .map(slot => slot.player?.id)
      .filter((id): id is number => id != null);

    this.api.saveLineup(starterPlayerIds).pipe(
      finalize(() => {
        this.saving = false;
      })
    ).subscribe({
      next: () => {
        this.savedStarterSlots = this.cloneSlots(this.starterSlots);
        this.savedBenchSlots = this.cloneSlots(this.benchSlots);
        this.selectedSlotId = null;
        this.successMessage = 'Lineup changes saved successfully.';
      },
      error: (err) => {
        this.errorMessage = err.error?.message || 'Failed to save lineup.';
      }
    });
  }

  resetToSaved(): void {
    if (!this.hasUnsavedChanges) {
      return;
    }

    this.starterSlots = this.cloneSlots(this.savedStarterSlots);
    this.benchSlots = this.cloneSlots(this.savedBenchSlots);
    this.selectedSlotId = null;
    this.errorMessage = null;
    this.successMessage = 'Draft reset to last saved lineup.';
  }

  isSelected(slot: LineupSlot): boolean {
    return this.selectedSlotId === slot.id;
  }

  private applyLineup(lineup: TeamLineupResponse): void {
    this.teamName = lineup.teamName;

    const starters = lineup.players
      .filter(p => p.starter)
      .sort((a, b) => this.positionOrder(a.position) - this.positionOrder(b.position) || a.name.localeCompare(b.name));
    const bench = lineup.players
      .filter(p => !p.starter)
      .sort((a, b) => this.positionOrder(a.position) - this.positionOrder(b.position) || a.name.localeCompare(b.name));

    this.starterSlots = this.starterSlots.map((slot, index) => ({
      ...slot,
      player: starters[index] ? { ...starters[index] } : null
    }));

    this.benchSlots = this.benchSlots.map((slot, index) => ({
      ...slot,
      player: bench[index] ? { ...bench[index] } : null
    }));

    this.savedStarterSlots = this.cloneSlots(this.starterSlots);
    this.savedBenchSlots = this.cloneSlots(this.benchSlots);
  }

  private cloneSlots(slots: LineupSlot[]): LineupSlot[] {
    return slots.map(slot => ({
      id: slot.id,
      player: slot.player ? { ...slot.player } : null
    }));
  }

  private serializeSlots(starters: LineupSlot[], bench: LineupSlot[]): string {
    const starterIds = starters.map(s => s.player?.id ?? 0).join(',');
    const benchIds = bench.map(s => s.player?.id ?? 0).join(',');
    return `${starterIds}|${benchIds}`;
  }

  private findSlotById(slotId: number): LineupSlot | null {
    return [...this.starterSlots, ...this.benchSlots].find(slot => slot.id === slotId) || null;
  }

  private validateFormation(): string | null {
    const starters = this.starterSlots.map(s => s.player).filter((p): p is TeamLineupPlayer => p != null);
    if (starters.length !== 11) {
      return 'Starting lineup must contain exactly 11 players.';
    }

    const gk = starters.filter(p => p.position === 'GK').length;
    const def = starters.filter(p => p.position === 'DEF').length;
    const fwd = starters.filter(p => p.position === 'FWD').length;

    if (gk !== 1) {
      return 'Formation error: exactly 1 goalkeeper is required.';
    }

    if (def < 3) {
      return 'Formation error: at least 3 defenders are required.';
    }

    if (fwd < 1) {
      return 'Formation error: at least 1 attacker is required.';
    }

    return null;
  }

  private positionOrder(position: string): number {
    if (position === 'GK') return 1;
    if (position === 'DEF') return 2;
    if (position === 'MID') return 3;
    if (position === 'FWD') return 4;
    return 99;
  }

  private getLockMessage(): string {
    const activeGw = this.transferWindowStatus?.activeGameweek || this.currentGameweek;
    if (activeGw) {
      return `You cannot make substitutions right now. We are currently in Gameweek ${activeGw}.`;
    }
    return 'You cannot make substitutions right now.';
  }

  private formatDeadline(deadline: string | null): string {
    if (!deadline) return '';

    const date = new Date(deadline);
    if (Number.isNaN(date.getTime())) return '';

    const now = new Date();
    const tomorrow = new Date(now);
    tomorrow.setDate(now.getDate() + 1);

    const sameDay = date.toDateString() === now.toDateString();
    const isTomorrow = date.toDateString() === tomorrow.toDateString();
    const timeLabel = date.toLocaleTimeString('en-GB', { hour: '2-digit', minute: '2-digit', hour12: false });

    if (sameDay) return `today at ${timeLabel}`;
    if (isTomorrow) return `tomorrow at ${timeLabel}`;

    return date.toLocaleString('en-GB', {
      day: '2-digit',
      month: 'short',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
      hour12: false
    });
  }
}
