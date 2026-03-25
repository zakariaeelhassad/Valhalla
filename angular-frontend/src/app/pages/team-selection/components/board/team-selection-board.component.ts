import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TransferWindowStatus } from '../../../../core/models';
import { getTeamJersey, getTeamLogo } from '../../../../shared/utils/team-visuals';
import { PitchSlot } from '../team-selection-types';

@Component({
  selector: 'app-team-selection-board',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './team-selection-board.component.html',
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
export class TeamSelectionBoardComponent {
  private readonly backendBase = 'http://localhost:8080';

  @Input() teamImage: string | null = null;
  @Input() playersSelected = 0;
  @Input() budget = 100;
  @Input() hasExistingTeam = false;
  @Input() currentGameweekTransferCount = 0;
  @Input() transfersMade = 0;
  @Input() freeTransfers = 1;
  @Input() transferCost = 0;

  @Input() loadingGameState = true;
  @Input() gameweekLocked = false;
  @Input() transferWindowStatus: TransferWindowStatus | null = null;
  @Input() currentGameweek = 0;
  @Input() nextDeadlineLabel = '';

  @Input() errorMessage: string | null = null;
  @Input() saving = false;
  @Input() activeSlotId: number | null = null;

  @Input() gks: PitchSlot[] = [];
  @Input() defs: PitchSlot[] = [];
  @Input() mids: PitchSlot[] = [];
  @Input() fwds: PitchSlot[] = [];

  @Output() slotOpen = new EventEmitter<PitchSlot>();
  @Output() slotRemove = new EventEmitter<PitchSlot>();
  @Output() resetTeamClick = new EventEmitter<void>();
  @Output() saveTeamClick = new EventEmitter<void>();
  @Output() resetTransfersClick = new EventEmitter<void>();
  @Output() saveTransfersClick = new EventEmitter<void>();

  onOpenSlot(slot: PitchSlot): void {
    this.slotOpen.emit(slot);
  }

  onRemoveSlot(slot: PitchSlot, event: Event): void {
    event.stopPropagation();
    this.slotRemove.emit(slot);
  }

  getTeamImageSrc(): string | null {
    const value = this.teamImage ?? null;
    if (!value) return null;
    if (value.startsWith('data:image/')) return value;
    if (value.startsWith('/')) return `${this.backendBase}${value}`;
    if (value.startsWith('http://') || value.startsWith('https://')) return value;
    return null;
  }

  getClubLogo(team: string | null | undefined): string {
    return getTeamLogo(team || '');
  }

  getJersey(team: string | null | undefined): string {
    return getTeamJersey(team || '');
  }
}
