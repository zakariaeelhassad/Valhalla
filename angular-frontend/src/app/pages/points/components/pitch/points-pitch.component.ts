import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { getTeamJersey, getTeamLogo } from '../../../../shared/utils/team-visuals';
import { PitchSquad, PointsPlayer } from '../points-types';

@Component({
  selector: 'app-points-pitch',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './points-pitch.component.html'
})
export class PointsPitchComponent {
  @Input() shouldShow = false;
  @Input() pitchSquad: PitchSquad = { gks: [], defs: [], mids: [], fwds: [] };
  @Input() benchSquad: PointsPlayer[] = [];

  getClubLogo(team: string | null | undefined): string {
    return getTeamLogo(team || '');
  }

  getJersey(team: string | null | undefined): string {
    return getTeamJersey(team || '');
  }
}
