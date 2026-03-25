import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, catchError, throwError, map, tap } from 'rxjs';
import {
    CurrentGameweekContext,
    DashboardStats,
    GameState,
    LeaderboardEntry,
    MatchResponse,
    PlayerSummary,
    PointsPageData,
    ProfileResponse,
    ProfileUpdateRequest,
    TeamGameweekStats,
    TeamLineupResponse,
    TeamResponse,
    TransferWindowStatus
} from '../models';

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
    getGameweekStats(gameweek: number): Observable<TeamGameweekStats> {
        return this.http.get<TeamGameweekStats>(`${BASE}/api/team/gameweek/${gameweek}`);
    }

    getCurrentGameweekTransferCount(): Observable<{ transferCount: number; gameweek: number }> {
        return this.http.get<{ transferCount: number; gameweek: number }>(`${BASE}/api/team/gameweek/transfers/count`).pipe(
            catchError(err => {
                console.error('ApiService.getCurrentGameweekTransferCount failed:', err);
                return throwError(() => err);
            })
        );
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

    getTeamLineup(): Observable<TeamLineupResponse> {
        return this.http.get<TeamLineupResponse>(`${BASE}/api/team/lineup`).pipe(
            catchError(err => {
                console.error('ApiService.getTeamLineup failed:', err);
                return throwError(() => err);
            })
        );
    }

    saveLineup(starterPlayerIds: number[]): Observable<void> {
        const payload = { starterPlayerIds };
        return this.http.post<void>(`${BASE}/api/team/lineup/save`, payload).pipe(
            catchError(err => {
                console.error('ApiService.saveLineup failed:', err);
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
    getCurrentGameweekContext(): Observable<CurrentGameweekContext> {
        return this.http.get<CurrentGameweekContext>(`${BASE}/api/matches/current`);
    }
    getTransferWindowStatus(): Observable<TransferWindowStatus> {
        return this.http.get<TransferWindowStatus>(`${BASE}/api/game/transfer-window`);
    }

    // ── Profile ───────────────────────────────────────
    getMyProfile(): Observable<ProfileResponse> {
        return this.http.get<ProfileResponse>(`${BASE}/api/profile`);
    }

    updateMyProfile(payload: ProfileUpdateRequest): Observable<ProfileResponse> {
        return this.http.put<ProfileResponse>(`${BASE}/api/profile`, payload);
    }
}
