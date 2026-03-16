import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, catchError, throwError, map, tap } from 'rxjs';

export interface PlayerSummary {
    id: number; name: string; position: string;
    realTeam: string; price: number; totalPoints: number;
}

export interface PaginatedResponse<T> {
    content: T[];
    totalElements: number;
    totalPages: number;
    size: number;
    number: number;
}

export interface TeamResponse {
    id: number; teamName: string; budget: number; remainingBudget: number;
    totalPoints: number; players: PlayerSummary[]; playerCount: number;
}
export interface TeamGameweekStats {
    gameweek: number;
    teamPoints: number;
    globalHighestPoints: number;
    players: { playerId: number; name: string; points: number; starter: boolean }[];
}
export interface GameweekResponse {
    id: number; gameweekNumber: number; startDate: string; endDate: string; status: string;
}
export interface MatchEvent {
    type: string; player: string; team: string; minute: number;
}

export interface MatchResponse {
    id: number; gameweekNumber: number; homeTeam: string; awayTeam: string;
    homeScore: number; awayScore: number; kickoffTime: string; finished: boolean;
    status: string; elapsedMinutes: number; events: MatchEvent[];
}
export interface SimClock {
    simulatedNow: string;
}

export interface GameState {
    currentGameweek: number; isRunning: boolean;
    gameweekActive: boolean;
    averagePoints: number; highestPoints: number;
    topManager: string; topManagerPoints: number;
}
export interface LeaderboardEntry {
    rank: number; userId: number; username: string; teamName: string; totalPoints: number;
}
export interface PlayerTransferStat {
    playerId: number; name: string; position: string; realTeam: string; transfers: number;
}
export interface Deadline {
    gameweek: number; deadlineTime: string; isNext: boolean;
}
export interface PlayerAvailability {
    playerId: number; name: string; realTeam: string; position: string; status: string; news: string; chanceOfPlaying: number;
}
export interface DashboardStats {
    gameState: GameState;
    topTransfersIn: PlayerTransferStat[];
    topTransfersOut: PlayerTransferStat[];
    deadlines: Deadline[];
    potw: PlayerSummary[];
    totw: PlayerSummary[];
    availability: PlayerAvailability[];
    wildcardsPlayed: number;
    transfersMade: number;
    mostTransferredInPlayer: string;
    mostCaptainedPlayer: string;
}
export interface UserLeague {
    id: number; name: string; userRank: number; type: 'classic' | 'broadcaster' | 'general' | 'cup' | 'h2h';
}
export interface PitchPlayer {
    player: PlayerSummary; isCaptain: boolean; isViceCaptain: boolean; multiplier: number; isSubstitite: boolean;
}
export interface AutoSub {
    playerOut: PlayerSummary; playerIn: PlayerSummary;
}
export interface Fixture {
    id: number; homeTeam: string; awayTeam: string; homeScore: number; awayScore: number; kickoffTime: string; status: 'Upcoming' | 'Live' | 'FT'; broadcasters: string[];
}
export interface PointsPageData {
    activeGameweek: number;
    gwPoints: number; averagePoints: number; highestPoints: number; gwRank: number; gwTransfers: number;
    leagues: UserLeague[];
    pitchPlayers: PitchPlayer[];
    benchPlayers: PitchPlayer[];
    autoSubs: AutoSub[];
    fixtures: Fixture[];
}

const BASE = 'http://localhost:8080';

@Injectable({ providedIn: 'root' })
export class ApiService {
    constructor(private http: HttpClient) { }

    // ── Team ──────────────────────────────────────────
    getMyTeam(): Observable<TeamResponse> {
        return this.http.get<TeamResponse>(`${BASE}/api/team`).pipe(
            catchError(err => {
                console.error('ApiService.getMyTeam failed:', err);
                return throwError(() => err);
            })
        );
    }
    addPlayer(id: number): Observable<TeamResponse> {
        return this.http.post<TeamResponse>(`${BASE}/api/team/players/${id}`, {});
    }
    removePlayer(id: number): Observable<TeamResponse> {
        return this.http.delete<TeamResponse>(`${BASE}/api/team/players/${id}`);
    }
    getSquadStats(): Observable<any> {
        return this.http.get<any>(`${BASE}/api/team/stats`);
    }
    getGameweekStats(gameweek: number): Observable<TeamGameweekStats> {
        return this.http.get<TeamGameweekStats>(`${BASE}/api/team/gameweek/${gameweek}`);
    }

    // ── Players ───────────────────────────────────────
    getPlayers(position?: string, size: number = 20): Observable<PlayerSummary[]> {
        let params = new HttpParams();
        if (position) params = params.set('position', position.toUpperCase());
        params = params.set('size', size.toString());

        console.log(`ApiService: Fetching players. Position=${position}, Size=${size}`);

        return this.http.get<any>(`${BASE}/api/players`, { params }).pipe(
            tap(res => console.log('ApiService: Response received', res)),
            map(response => {
                if (Array.isArray(response)) {
                    console.log('ApiService: Returning flat array');
                    return response;
                }
                if (response && response.content && Array.isArray(response.content)) {
                    console.log('ApiService: Returning Page.content array');
                    return response.content;
                }
                console.warn('ApiService: Unexpected response structure!', response);
                return [];
            }),
            catchError(err => {
                console.error(`ApiService.getPlayers failed:`, err);
                return throwError(() => err);
            })
        );
    }

    saveTeam(playerIds: number[]): Observable<TeamResponse> {
        return this.http.post<TeamResponse>(`${BASE}/api/team/save`, playerIds).pipe(
            catchError(err => {
                console.error('ApiService.saveTeam failed:', err);
                return throwError(() => err);
            })
        );
    }

    saveTransfers(playerIds: number[], cost: number): Observable<any> {
        const payload = { playerIds, cost };
        return this.http.post<any>(`${BASE}/api/team/transfers`, payload).pipe(
            catchError(err => {
                console.error('ApiService.saveTransfers failed:', err);
                return throwError(() => err);
            })
        );
    }


    // ── Game ──────────────────────────────────────────
    getGameState(): Observable<GameState> {
        return this.http.get<GameState>(`${BASE}/api/game/state`);
    }
    getLeaderboard(): Observable<LeaderboardEntry[]> {
        return this.http.get<LeaderboardEntry[]>(`${BASE}/api/game/leaderboard`);
    }
    getDashboardStats(): Observable<DashboardStats> {
        return this.http.get<DashboardStats>(`${BASE}/api/game/dashboard-stats`);
    }
    getPointsPageData(gameweek?: number): Observable<PointsPageData> {
        let params = new HttpParams();
        if (gameweek) params = params.set('gw', gameweek.toString());
        return this.http.get<PointsPageData>(`${BASE}/api/game/points-data`, { params });
    }

    // ── Matches ───────────────────────────────────────
    getMatchesByGameweek(gw: number): Observable<MatchResponse[]> {
        return this.http.get<MatchResponse[]>(`${BASE}/api/matches/gameweek/${gw}`);
    }
    getLiveMatches(): Observable<MatchResponse[]> {
        return this.http.get<MatchResponse[]>(`${BASE}/api/matches/live`);
    }
    getSimClock(): Observable<SimClock> {
        return this.http.get<SimClock>(`${BASE}/api/matches/clock`);
    }
}
