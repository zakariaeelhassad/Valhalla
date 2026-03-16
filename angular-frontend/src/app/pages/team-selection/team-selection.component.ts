import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { ApiService, PlayerSummary, GameState } from '../../core/services/api.service';
import { NavbarComponent } from '../../shared/components/navbar/navbar.component';
import { FormsModule } from '@angular/forms';
import { finalize } from 'rxjs';

export interface PitchSlot {
  id: number;
  position: 'GK' | 'DEF' | 'MID' | 'FWD';
  player: PlayerSummary | null;
}

@Component({
  selector: 'app-team-selection',
  standalone: true,
  imports: [CommonModule, NavbarComponent, FormsModule],
  templateUrl: './team-selection.component.html',
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
export class TeamSelectionComponent implements OnInit {
  budget: number = 100.0;
  playersSelected: number = 0;
  maxPlayers: number = 15;
  errorMessage: string | null = null;
  saving: boolean = false;
  currentGameweek: number = 0;

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

  // Transfer Window Lock State
  gameweekLocked: boolean = false;
  loadingGameState: boolean = true;

  constructor(private api: ApiService, private router: Router, private cdr: ChangeDetectorRef) { }

  ngOnInit(): void {
    console.log('TeamSelectionComponent: Initializing, checking for existing team...');

    // Fetch game state first to determine if transfer window is open
    this.api.getGameState().subscribe({
      next: (gs: GameState) => {
        this.currentGameweek = gs.currentGameweek;
        this.gameweekLocked = gs.gameweekActive === true;
        this.loadingGameState = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.gameweekLocked = false; // Fail open if endpoint unreachable
        this.loadingGameState = false;
      }
    });

    this.api.getMyTeam().subscribe({
      next: (team) => {
        if (team && team.players && team.players.length === 15) {
          console.log('TeamSelectionComponent: Found existing full team, loading into slots.');
          this.hasExistingTeam = true;
          this.budget = team.remainingBudget;
          this.originalBudget = team.remainingBudget;
          this.playersSelected = 15;
          this.originalTeam = [...team.players]; // Save copy for transfer maths

          // Populate slots
          const playerMap = new Map<string, PlayerSummary[]>();
          team.players.forEach(p => {
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

          this.fetchPlayers(); // Fetch available list for view only
        } else {
          // Team exists but maybe incomplete, treat as blank slate and start over (or handle edge case)
          this.hasExistingTeam = false;
          this.resetTeam();
        }
      },
      error: (err) => {
        console.log('TeamSelectionComponent: No existing team found, initializing blank slate.');
        this.hasExistingTeam = false;
        this.resetTeam();
      }
    });
  }

  get gks() { return this.slots.filter(s => s.position === 'GK'); }
  get defs() { return this.slots.filter(s => s.position === 'DEF'); }
  get mids() { return this.slots.filter(s => s.position === 'MID'); }
  get fwds() { return this.slots.filter(s => s.position === 'FWD'); }

  openPlayerModal(slot: PitchSlot) {
    if (this.gameweekLocked && this.hasExistingTeam) {
      this.errorMessage = `Transfers are locked until Gameweek ${this.currentGameweek} finishes.`;
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

  filterPlayers() {
    let filtered = [...this.availablePlayers];
    console.log(`TeamSelectionComponent: Starting filter. totalPlayers=${filtered.length}, activeFilter=${this.activePositionFilter}`);

    // Filter by position if active
    if (this.activePositionFilter) {
      filtered = filtered.filter(p => p.position === this.activePositionFilter);
      console.log(`TeamSelectionComponent: After position filter (${this.activePositionFilter}): ${filtered.length}`);
    }

    // Filter out already selected players
    const selectedIds = this.slots.map(s => s.player?.id).filter(id => id !== undefined);
    filtered = filtered.filter(p => !selectedIds.includes(p.id));
    console.log(`TeamSelectionComponent: After excluding selected: ${filtered.length}`);

    if (this.searchQuery.trim() !== '') {
      const q = this.searchQuery.toLowerCase();
      filtered = filtered.filter(p => p.name.toLowerCase().includes(q));
      this.currentPage = 1;
      console.log(`TeamSelectionComponent: After search filter: ${filtered.length}`);
    }

    this.filteredPlayers = filtered;
    this.totalPages = Math.ceil(this.filteredPlayers.length / this.pageSize) || 1;

    if (this.currentPage > this.totalPages) {
      this.currentPage = 1;
    }

    this.updatePagination();
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
    if (this.gameweekLocked && this.hasExistingTeam) return; // Locked during active gameweek
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
    const extraTransfers = this.transfersMade - this.freeTransfers;
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
      this.errorMessage = `You cannot save. Please wait until Gameweek ${this.currentGameweek} finishes.`;
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
}
