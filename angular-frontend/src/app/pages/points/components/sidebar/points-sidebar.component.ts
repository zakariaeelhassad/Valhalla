import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { UserResponse } from '../../../../core/services/auth.service';
import { TeamResponse } from '../../../../core/models';

@Component({
  selector: 'app-points-sidebar',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './points-sidebar.component.html'
})
export class PointsSidebarComponent {
  private readonly backendBase = 'http://localhost:8081';

  @Input() user: UserResponse | null = null;
  @Input() teamResponse: TeamResponse | null = null;
  @Input() gwPoints = 0;
  @Input() hideGameweekPoints = false;
  @Input() teamName: string | null = null;
  @Input() remainingBudget: number | null = null;

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
