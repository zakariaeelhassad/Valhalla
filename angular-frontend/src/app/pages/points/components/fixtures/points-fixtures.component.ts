import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatchResponse } from '../../../../core/models';
import { getTeamLogo } from '../../../../shared/utils/team-visuals';

@Component({
  selector: 'app-points-fixtures',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './points-fixtures.component.html'
})
export class PointsFixturesComponent {
  @Input() matches: MatchResponse[] = [];
  @Input() viewedGameweek = 0;
  @Input() dbCurrentGameweek = 0;
  @Input() backendCurrentDate = '';

  @Output() gameweekChange = new EventEmitter<number>();

  changeGameweek(delta: number): void {
    this.gameweekChange.emit(delta);
  }

  get matchesByDate(): { date: string; matches: MatchResponse[] }[] {
    const grouped: { [key: string]: MatchResponse[] } = {};
    this.matches.forEach(m => {
      const d = m.kickoffTime ? m.kickoffTime.slice(0, 10) : 'TBD';
      if (!grouped[d]) grouped[d] = [];
      grouped[d].push(m);
    });
    return Object.keys(grouped).sort().map(date => {
      return {
        date: date !== 'TBD'
          ? new Date(date + 'T12:00:00').toLocaleDateString('en-GB', { weekday: 'long', day: 'numeric', month: 'long', year: 'numeric' })
          : 'TBD',
        matches: grouped[date]
      };
    });
  }

  getAbbr(team: string): string {
    return team ? team.substring(0, 3).toUpperCase() : '';
  }

  getClubLogo(team: string | null | undefined): string {
    return getTeamLogo(team || '');
  }

  getTeamColor(team: string): string {
    const palette = ['#6366f1', '#8b5cf6', '#ec4899', '#f43f5e', '#f97316', '#eab308', '#22c55e', '#14b8a6', '#06b6d4', '#3b82f6', '#10b981', '#84cc16'];
    let hash = 0;
    if (team) {
      for (let i = 0; i < team.length; i++) {
        hash = team.charCodeAt(i) + ((hash << 5) - hash);
      }
    }
    return palette[Math.abs(hash) % palette.length];
  }

  formatKickoff(isoTime: string): string {
    if (!isoTime) return '';
    const ko = new Date(isoTime);
    return ko.toLocaleTimeString('en-GB', { hour: '2-digit', minute: '2-digit' });
  }

  getVisibleEvents(match: MatchResponse) {
    if (!match.events || match.events.length === 0) return [];
    const uiStatus = this.getMatchDisplayStatus(match);
    if (uiStatus === 'FINISHED') return match.events;
    if (uiStatus === 'LIVE') {
      return match.events.filter(e => e.minute <= this.getDisplayElapsedMinutes(match));
    }
    return [];
  }

  getLiveHomeScore(match: MatchResponse): number {
    const visibleEvents = this.getVisibleEvents(match);
    return visibleEvents.filter(e => e.type === 'GOAL' && e.team === match.homeTeam).length;
  }

  getLiveAwayScore(match: MatchResponse): number {
    const visibleEvents = this.getVisibleEvents(match);
    return visibleEvents.filter(e => e.type === 'GOAL' && e.team === match.awayTeam).length;
  }

  getDisplayHomeScore(match: MatchResponse): number {
    const uiStatus = this.getMatchDisplayStatus(match);
    if (uiStatus === 'LIVE') {
      return this.getLiveHomeScore(match);
    }
    if (uiStatus === 'FINISHED') {
      const goalsFromEvents = this.getLiveHomeScore(match);
      return goalsFromEvents > 0 ? goalsFromEvents : (match.homeScore || 0);
    }
    return match.homeScore || 0;
  }

  getDisplayAwayScore(match: MatchResponse): number {
    const uiStatus = this.getMatchDisplayStatus(match);
    if (uiStatus === 'LIVE') {
      return this.getLiveAwayScore(match);
    }
    if (uiStatus === 'FINISHED') {
      const goalsFromEvents = this.getLiveAwayScore(match);
      return goalsFromEvents > 0 ? goalsFromEvents : (match.awayScore || 0);
    }
    return match.awayScore || 0;
  }

  getMatchStat(match: MatchResponse, team: 'home' | 'away', stat: 'goals' | 'yellows' | 'reds'): number {
    const teamName = team === 'home' ? match.homeTeam : match.awayTeam;
    const visibleEvents = this.getVisibleEvents(match);

    if (stat === 'goals') {
      return visibleEvents.filter(e => e.type === 'GOAL' && e.team === teamName).length;
    }
    if (stat === 'yellows') {
      return visibleEvents.filter(e => e.type === 'YELLOW_CARD' && e.team === teamName).length;
    }
    return visibleEvents.filter(e => e.type === 'RED_CARD' && e.team === teamName).length;
  }

  getMatchDisplayStatus(match: MatchResponse): 'SCHEDULED' | 'LIVE' | 'FINISHED' {
    if (!match.kickoffTime) return 'SCHEDULED';

    const kickoff = new Date(match.kickoffTime);
    const now = this.getEffectiveNow();
    const liveEnd = new Date(kickoff.getTime() + 105 * 60 * 1000);

    if (now < kickoff) return 'SCHEDULED';
    if (now >= kickoff && now < liveEnd) return 'LIVE';
    return 'FINISHED';
  }

  getDisplayElapsedMinutes(match: MatchResponse): number {
    const uiStatus = this.getMatchDisplayStatus(match);
    if (uiStatus !== 'LIVE') return 0;

    const kickoff = new Date(match.kickoffTime);
    const now = this.getEffectiveNow();
    const minutes = Math.floor((now.getTime() - kickoff.getTime()) / 60000);
    return Math.max(0, Math.min(minutes, 90));
  }

  private getEffectiveNow(): Date {
    if (this.backendCurrentDate) {
      const backendNow = new Date(this.backendCurrentDate);
      if (!Number.isNaN(backendNow.getTime())) {
        return backendNow;
      }
    }
    return new Date();
  }
}
