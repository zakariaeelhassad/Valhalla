import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { NavbarComponent } from '../../../../shared/components/navbar/navbar.component';
import { UserResponse } from '../../../../core/services/auth.service';
import { MatchResponse, TeamResponse } from '../../../../core/models';
import { PointsStateService } from '../../../../core/services/points-state.service';
import { PointsSidebarComponent } from '../sidebar/points-sidebar.component';
import { PointsGameweekSummaryComponent } from '../gameweek-summary/points-gameweek-summary.component';
import { PointsPitchComponent } from '../pitch/points-pitch.component';
import { PointsFixturesComponent } from '../fixtures/points-fixtures.component';
import { PitchSquad, PointsPlayer } from '../points-types';

@Component({
  selector: 'app-points-content',
  standalone: true,
  imports: [
    CommonModule,
    NavbarComponent,
    PointsSidebarComponent,
    PointsGameweekSummaryComponent,
    PointsPitchComponent,
    PointsFixturesComponent
  ],
  templateUrl: './points-content.component.html'
})
export class PointsContentComponent {
  private readonly backendBase = 'http://localhost:8080';

  @Input() user: UserResponse | null = null;
  @Input() teamResponse: TeamResponse | null = null;
  @Input() gwPoints = 0;
  @Input() highestPoints = 0;
  @Input() mySquad: PointsPlayer[] = [];
  @Input() pitchSquad: PitchSquad = {
    gks: [], defs: [], mids: [], fwds: []
  };
  @Input() benchSquad: PointsPlayer[] = [];
  @Input() matches: MatchResponse[] = [];
  @Input() viewedGameweek = 0;
  @Input() dbCurrentGameweek = 0;
  @Input() backendCurrentDate = '';

  @Output() gameweekChange = new EventEmitter<number>();

  constructor(public state: PointsStateService) {}

  changeGameweek(delta: number): void {
    this.gameweekChange.emit(delta);
  }

  isFutureHiddenGameweek(): boolean {
    const viewed = this.viewedGameweek || this.dbCurrentGameweek || 1;
    const maxVisible = this.dbCurrentGameweek || 1;
    return viewed > maxVisible;
  }

  shouldShowLineupSections(): boolean {
    return !this.isFutureHiddenGameweek() && this.mySquad.length > 0;
  }

  getTeamImageSrc(): string | null {
    const value = this.teamResponse?.teamImage ?? null;
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
}
