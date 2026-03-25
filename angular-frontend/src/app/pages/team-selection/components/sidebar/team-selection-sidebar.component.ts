import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { PlayerSummary } from '../../../../core/models';
import { getTeamLogo } from '../../../../shared/utils/team-visuals';

@Component({
  selector: 'app-team-selection-sidebar',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './team-selection-sidebar.component.html',
  styles: [
    `
      .glass-card {
        background: rgba(30, 41, 59, 0.4);
        backdrop-filter: blur(12px);
        -webkit-backdrop-filter: blur(12px);
        border: 1px solid rgba(255, 255, 255, 0.08);
        border-radius: 1rem;
        box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1), 0 2px 4px -1px rgba(0, 0, 0, 0.06);
      }
    `
  ]
})
export class TeamSelectionSidebarComponent {
  @Input() activeFilterPanel: 'global' | 'position' | 'teams' | null = null;
  @Input() sortBy: 'points' | 'price' | 'form' = 'points';
  @Input() priceFilter: number | null = null;
  @Input() selectedPosition: 'GK' | 'DEF' | 'MID' | 'FWD' | null = null;
  @Input() selectedTeam: string | null = null;
  @Input() uniqueTeams: string[] = [];

  @Input() searchQuery = '';
  @Input() loadingPlayers = false;
  @Input() filteredPlayers: PlayerSummary[] = [];
  @Input() paginatedPlayers: PlayerSummary[] = [];
  @Input() currentPage = 1;
  @Input() totalPages = 1;
  @Input() errorMessage: string | null = null;

  @Output() filterPanelToggle = new EventEmitter<'global' | 'position' | 'teams'>();
  @Output() filtersReset = new EventEmitter<void>();
  @Output() sortByChange = new EventEmitter<'points' | 'price' | 'form'>();
  @Output() priceFilterChange = new EventEmitter<number | null>();
  @Output() positionFilterChange = new EventEmitter<'GK' | 'DEF' | 'MID' | 'FWD' | null>();
  @Output() teamFilterChange = new EventEmitter<string | null>();
  @Output() searchQueryChange = new EventEmitter<string>();
  @Output() playerSelected = new EventEmitter<PlayerSummary>();
  @Output() prevPageClick = new EventEmitter<void>();
  @Output() nextPageClick = new EventEmitter<void>();

  getClubLogo(team: string | null | undefined): string {
    return getTeamLogo(team || '');
  }

  onSearchChange(value: string): void {
    this.searchQueryChange.emit(value);
  }
}
