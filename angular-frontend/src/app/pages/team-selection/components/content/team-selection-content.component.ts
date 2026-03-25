import { Component, OnDestroy, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { ApiService } from '../../../../core/services/api.service';
import { PlayerSummary, TransferWindowStatus } from '../../../../core/models';
import { catchError, finalize, forkJoin, interval, of, startWith, Subscription, switchMap } from 'rxjs';
import { NavbarComponent } from '../../../../shared/components/navbar/navbar.component';
import { TeamSelectionSidebarComponent } from '../sidebar/team-selection-sidebar.component';
import { TeamSelectionBoardComponent } from '../board/team-selection-board.component';
import { PitchSlot } from '../team-selection-types';

@Component({
  selector: 'app-team-selection-content',
  standalone: true,
  imports: [CommonModule, NavbarComponent, TeamSelectionSidebarComponent, TeamSelectionBoardComponent],
  templateUrl: './team-selection-content.component.html',
  styles: []
})
export class TeamSelectionContentComponent implements OnInit, OnDestroy {
  budget: number = 100.0;
  playersSelected: number = 0;
  maxPlayers: number = 15;
  errorMessage: string | null = null;
  saving: boolean = false;
  currentGameweek: number = 0;
  teamImage: string | null = null;

  slots: PitchSlot[] = [
    { id: 1, position: 'GK', player: null },
    { id: 2, position: 'GK', player: null },
    { id: 3, position: 'DEF', player: null },
    { id: 4, position: 'DEF', player: null },
    { id: 5, position: 'DEF', player: null },
    { id: 6, position: 'DEF', player: null },
    { id: 7, position: 'DEF', player: null },
    { id: 8, position: 'MID', player: null },
    { id: 9, position: 'MID', player: null },
    { id: 10, position: 'MID', player: null },
    { id: 11, position: 'MID', player: null },
    { id: 12, position: 'MID', player: null },
    { id: 13, position: 'FWD', player: null },
    { id: 14, position: 'FWD', player: null },
    { id: 15, position: 'FWD', player: null },
  ];

  // Modal State
  isModalOpen: boolean = false;
  activeSlotId: number | null = null;
  activePositionFilter: 'GK' | 'DEF' | 'MID' | 'FWD' | null = null;

  availablePlayers: PlayerSummary[] = [];
  filteredPlayers: PlayerSummary[] = [];
  paginatedPlayers: PlayerSummary[] = [];
  searchQuery: string = '';
  loadingPlayers: boolean = false;

  // Pagination State
  currentPage: number = 1;
  pageSize: number = 10;
  totalPages: number = 1;

  hasExistingTeam: boolean = false;

  // Transfer Mechanics State
  originalTeam: PlayerSummary[] = [];
  originalBudget: number = 100.0;
  freeTransfers: number = 1;
  currentGameweekTransferCount: number = 0; // Actual transfer count from backend for this gameweek

  // Transfer Window Lock State
  gameweekLocked: boolean = false;
  loadingGameState: boolean = true;
  transferWindowStatus: TransferWindowStatus | null = null;
  nextDeadlineLabel: string = '';
  private transferWindowSub?: Subscription;

  // Filter State
  filterView: 'all' | 'watchlist' = 'all';
  sortBy: 'points' | 'price' | 'form' = 'points';
  priceFilter: number | null = null;
  selectedPosition: 'GK' | 'DEF' | 'MID' | 'FWD' | null = null;
  selectedTeam: string | null = null;

  // Unique teams list for filtering
  uniqueTeams: string[] = [];
  activeFilterPanel: 'global' | 'position' | 'teams' | null = null;

  constructor(private api: ApiService, private router: Router, private cdr: ChangeDetectorRef) { }

  ngOnInit(): void {
    console.log('TeamSelectionComponent: Initializing, checking for existing team...');

    // Keep lock status aligned with backend time and DB gameweek dates.
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

    forkJoin({
      team: this.api.getMyTeam().pipe(catchError(() => of(null))),
      lineup: this.api.getTeamLineup().pipe(catchError(() => of(null)))
    }).subscribe({
      next: ({ team, lineup }) => {
        const lineupPlayers: PlayerSummary[] = (lineup?.players || []).map(p => ({
          id: p.id,
          name: p.name,
          position: p.position,
          realTeam: p.realTeam,
          price: p.price,
          totalPoints: p.totalPoints
        }));

        if (team) {
          this.teamImage = team.teamImage || null;
          this.budget = team.remainingBudget;
          this.originalBudget = team.remainingBudget;
        }

        if (lineupPlayers.length === 15) {
          console.log('TeamSelectionComponent: Found full lineup, loading into slots.');
          this.hasExistingTeam = true;
          this.playersSelected = 15;
          this.originalTeam = [...lineupPlayers];

          const playerMap = new Map<string, PlayerSummary[]>();
          lineupPlayers.forEach(p => {
            const key = p.position;
            if (!playerMap.has(key)) playerMap.set(key, []);
            playerMap.get(key)!.push(p);
          });

          this.slots.forEach(slot => {
            const availableForPos = playerMap.get(slot.position);
            if (availableForPos && availableForPos.length > 0) {
              slot.player = availableForPos.shift() || null;
            }
          });

          this.fetchPlayers();
          this.fetchCurrentGameweekTransferCount();
        } else {
          this.hasExistingTeam = false;
          this.resetTeam();
        }
      },
      error: () => {
        console.log('TeamSelectionComponent: Failed to load team data, initializing blank slate.');
        this.hasExistingTeam = false;
        this.resetTeam();
      }
    });
  }

  ngOnDestroy(): void {
    this.transferWindowSub?.unsubscribe();
  }

  get gks() { return this.slots.filter(s => s.position === 'GK'); }
  get defs() { return this.slots.filter(s => s.position === 'DEF'); }
  get mids() { return this.slots.filter(s => s.position === 'MID'); }
  get fwds() { return this.slots.filter(s => s.position === 'FWD'); }

  openPlayerModal(slot: PitchSlot) {
    if (this.gameweekLocked) {
      this.errorMessage = this.getLockMessage();
      return;
    }
    if (slot.player) return; // Slot is full
    this.activeSlotId = slot.id;
    this.activePositionFilter = slot.position;
    this.isModalOpen = true;
    this.errorMessage = null;
    this.fetchPlayers(slot.position);
  }

  closeModal() {
    this.isModalOpen = false;
    this.activeSlotId = null;
    this.activePositionFilter = null;
    this.searchQuery = '';
    this.fetchPlayers(); // Restore "All Players" list
  }

  fetchPlayers(position?: string) {
    console.log(`TeamSelectionComponent: Fetching players for position: ${position || 'ALL'}`);
    this.loadingPlayers = true;
    this.errorMessage = null;

    this.api.getPlayers(position, 500)
      .subscribe({
        next: (players) => {
          this.loadingPlayers = false;
          console.log(`TeamSelectionComponent: Received ${players?.length || 0} players.`);
          this.availablePlayers = players || [];
          this.buildUniqueTeams();
          this.currentPage = 1;
          this.filterPlayers();
          this.cdr.detectChanges();
        },
        error: (err) => {
          this.loadingPlayers = false;
          console.error(`Fetch failed for ${position || 'all'}:`, err);
          this.errorMessage = `Failed to load players. Please check your connection and try again.`;
          this.availablePlayers = [];
          this.filterPlayers();
          this.cdr.detectChanges();
        }
      });
  }

  fetchCurrentGameweekTransferCount() {
    this.api.getCurrentGameweekTransferCount().subscribe({
      next: (response) => {
        this.currentGameweekTransferCount = response.transferCount;
        console.log(`TeamSelectionComponent: Current gameweek (${response.gameweek}) transfer count: ${response.transferCount}`);
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error('Failed to fetch current gameweek transfer count:', err);
        this.currentGameweekTransferCount = 0;
      }
    });
  }

  filterPlayers() {
    let filtered = [...this.availablePlayers];
    console.log(`TeamSelectionComponent: Starting filter. totalPlayers=${filtered.length}, activeFilter=${this.activePositionFilter}`);

    // Filter by position (from modal or filter selection)
    if (this.activePositionFilter) {
      filtered = filtered.filter(p => p.position === this.activePositionFilter);
    } else if (this.selectedPosition) {
      filtered = filtered.filter(p => p.position === this.selectedPosition);
    }

    // Filter by team if selected
    if (this.selectedTeam) {
      filtered = filtered.filter(p => p.realTeam === this.selectedTeam);
    }

    // Filter by price if set
    if (this.priceFilter !== null) {
      filtered = filtered.filter(p => p.price <= this.priceFilter!);
    }

    // Filter out already selected players
    const selectedIds = this.slots.map(s => s.player?.id).filter(id => id !== undefined);
    filtered = filtered.filter(p => !selectedIds.includes(p.id));
    console.log(`TeamSelectionComponent: After excluding selected: ${filtered.length}`);

    // Apply search query
    if (this.searchQuery.trim() !== '') {
      const q = this.searchQuery.toLowerCase();
      filtered = filtered.filter(p => p.name.toLowerCase().includes(q));
      this.currentPage = 1;
      console.log(`TeamSelectionComponent: After search filter: ${filtered.length}`);
    }

    // Sort players
    filtered.sort((a, b) => {
      if (this.sortBy === 'points') {
        return b.totalPoints - a.totalPoints;
      } else if (this.sortBy === 'price') {
        return b.price - a.price;
      }
      return 0;
    });

    this.filteredPlayers = filtered;
    this.totalPages = Math.ceil(this.filteredPlayers.length / this.pageSize) || 1;

    if (this.currentPage > this.totalPages) {
      this.currentPage = 1;
    }

    this.updatePagination();
  }

  // Filter helper methods
  toggleFilterPanel(panel: 'global' | 'position' | 'teams') {
    this.activeFilterPanel = this.activeFilterPanel === panel ? null : panel;
  }

  setPositionFilter(position: 'GK' | 'DEF' | 'MID' | 'FWD' | null) {
    this.selectedPosition = this.selectedPosition === position ? null : position;
    this.currentPage = 1;
    this.filterPlayers();
    this.activeFilterPanel = null;
    this.cdr.detectChanges();
  }

  setTeamFilter(team: string | null) {
    this.selectedTeam = this.selectedTeam === team ? null : team;
    this.currentPage = 1;
    this.filterPlayers();
    this.activeFilterPanel = null;
    this.cdr.detectChanges();
  }

  setPriceFilter(price: number | null) {
    this.priceFilter = price;
    this.currentPage = 1;
    this.filterPlayers();
    this.cdr.detectChanges();
  }

  setSortBy(sort: 'points' | 'price' | 'form') {
    this.sortBy = sort;
    this.currentPage = 1;
    this.filterPlayers();
    this.cdr.detectChanges();
  }

  setFilterView(view: 'all' | 'watchlist') {
    this.filterView = view;
    this.currentPage = 1;
    this.filterPlayers();
    this.cdr.detectChanges();
  }

  resetFilters() {
    this.filterView = 'all';
    this.sortBy = 'points';
    this.priceFilter = null;
    this.selectedPosition = null;
    this.selectedTeam = null;
    this.activeFilterPanel = null;
    this.searchQuery = '';
    this.currentPage = 1;
    this.fetchPlayers();
  }

  // Build unique teams list when players load
  buildUniqueTeams() {
    const teams = new Set(this.availablePlayers.map(p => p.realTeam));
    this.uniqueTeams = Array.from(teams).sort();
  }

  updatePagination() {
    const start = (this.currentPage - 1) * this.pageSize;
    const end = start + this.pageSize;
    this.paginatedPlayers = this.filteredPlayers.slice(start, end);
  }

  nextPage() {
    if (this.currentPage < this.totalPages) {
      this.currentPage++;
      this.updatePagination();
    }
  }

  prevPage() {
    if (this.currentPage > 1) {
      this.currentPage--;
      this.updatePagination();
    }
  }

  selectPlayer(player: PlayerSummary) {
    this.errorMessage = null;

    if (this.budget < player.price) {
      this.errorMessage = `Insufficient budget. £${player.price}m required, but only £${this.budget.toFixed(1)}m available.`;
      return;
    }

    // Check Max 3 players per team rule
    const teamCount = this.slots.reduce((count, slot) => {
      if (slot.player && slot.player.realTeam === player.realTeam) {
        return count + 1;
      }
      return count;
    }, 0);

    if (teamCount >= 3) {
      this.errorMessage = `Too many players selected from ${player.realTeam} (Max 3).`;
      return;
    }

    // Add to slot
    let slotIndex = -1;
    if (this.activeSlotId) {
      slotIndex = this.slots.findIndex(s => s.id === this.activeSlotId);
    } else {
      // Find first empty slot for this player's position
      slotIndex = this.slots.findIndex(s => s.position === player.position && !s.player);
    }

    if (slotIndex !== -1) {
      this.slots[slotIndex].player = player;
      this.budget = parseFloat((this.budget - player.price).toFixed(1));
      this.playersSelected++;
      if (this.isModalOpen) {
        this.closeModal();
      } else {
        this.filterPlayers(); // Refresh list to remove selected player
      }
    } else {
      this.errorMessage = `No empty ${player.position} slots available.`;
    }
  }

  removePlayer(slot: PitchSlot, event?: Event) {
    if (event) event.stopPropagation();
    if (this.gameweekLocked) {
      this.errorMessage = this.getLockMessage();
      return;
    }
    if (!slot.player) return;

    this.budget = parseFloat((this.budget + slot.player.price).toFixed(1));
    this.playersSelected--;
    slot.player = null;
    this.errorMessage = null;
  }

  resetTeam() {
    this.slots.forEach(slot => {
      slot.player = null;
    });
    this.budget = 100.0;
    this.playersSelected = 0;
    this.errorMessage = null;
    this.activeSlotId = null;
    this.activePositionFilter = null;
    this.searchQuery = '';
    this.fetchPlayers();
  }

  saveTeam() {
    if (this.gameweekLocked) {
      this.errorMessage = this.getLockMessage();
      return;
    }

    if (this.playersSelected < 15) return;
    this.saving = true;
    this.errorMessage = null;

    const playerIds = this.slots.map(s => s.player!.id);

    this.api.saveTeam(playerIds)
      .pipe(
        finalize(() => {
          this.saving = false;
        })
      )
      .subscribe({
        next: () => {
          this.router.navigate(['/points']);
        },
        error: (err) => {
          console.error('Save failed:', err);
          this.errorMessage = err.error?.message || 'Failed to save team. Please try again.';
        }
      });
  }

  // --- Transfer Maths ---
  get transfersMade(): number {
    if (!this.hasExistingTeam) return 0;

    // Count players in current squad that are NOT in the original squad
    const originalIds = this.originalTeam.map(p => p.id);
    const currentIds = this.slots.map(s => s.player?.id).filter(id => id !== undefined);

    let newPlayersCount = 0;
    for (const id of currentIds) {
      if (!originalIds.includes(id as number)) {
        newPlayersCount++;
      }
    }
    return newPlayersCount;
  }

  get transferCost(): number {
    if (!this.hasExistingTeam) return 0;
    // Include both the transfer count from this session and any already made in this gameweek
    const totalTransfers = this.currentGameweekTransferCount + this.transfersMade;
    const extraTransfers = totalTransfers - this.freeTransfers;
    return Math.max(0, extraTransfers * 4);
  }

  resetTransfers() {
    this.slots.forEach(slot => slot.player = null);
    this.budget = this.originalBudget;
    this.playersSelected = 15;

    // Re-populate from originalTeam
    const playerMap = new Map<string, PlayerSummary[]>();
    this.originalTeam.forEach(p => {
      const key = p.position;
      if (!playerMap.has(key)) playerMap.set(key, []);
      playerMap.get(key)!.push({ ...p });
    });

    this.slots.forEach(slot => {
      const availableForPos = playerMap.get(slot.position);
      if (availableForPos && availableForPos.length > 0) {
        slot.player = availableForPos.shift() || null;
      }
    });

    this.errorMessage = null;
    this.activeSlotId = null;
    this.activePositionFilter = null;
    this.searchQuery = '';
    this.fetchPlayers();
  }

  saveTransfers() {
    if (this.gameweekLocked) {
      this.errorMessage = this.getLockMessage();
      return;
    }
    if (this.playersSelected < 15) {
      this.errorMessage = "You must have exactly 15 players selected to make transfers.";
      return;
    }

    if (this.transfersMade === 0) {
      this.errorMessage = "No transfers were made.";
      return;
    }

    this.saving = true;
    this.errorMessage = null;

    const playerIds = this.slots.map(s => s.player!.id);

    // Call the new API method passing the transfer cost
    this.api.saveTransfers(playerIds, this.transferCost)
      .pipe(
        finalize(() => {
          this.saving = false;
        })
      )
      .subscribe({
        next: () => {
          this.router.navigate(['/points']);
        },
        error: (err) => {
          console.error('Transfers failed', err);
          this.saving = false; // Safety net in case finalize doesn't fire
          this.errorMessage = err.error?.message || 'Failed to save transfers. Please try again.';
        }
      });
  }

  private getLockMessage(): string {
    const activeGw = this.transferWindowStatus?.activeGameweek || this.currentGameweek;
    if (activeGw) {
      return `Transfers are locked. We are currently in Gameweek ${activeGw}.`;
    }
    return 'Transfers are currently locked.';
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
