import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable, forkJoin, catchError, of, tap } from 'rxjs';
import { ApiService, TeamResponse, DashboardStats } from './api.service';

@Injectable({
    providedIn: 'root'
})
export class DashboardStateService {
    private _team = new BehaviorSubject<TeamResponse | null>(null);
    private _dashboardStats = new BehaviorSubject<DashboardStats | null>(null);
    private _loading = new BehaviorSubject<boolean>(true);
    private _error = new BehaviorSubject<string | null>(null);

    team$: Observable<TeamResponse | null> = this._team.asObservable();
    dashboardStats$: Observable<DashboardStats | null> = this._dashboardStats.asObservable();
    loading$: Observable<boolean> = this._loading.asObservable();
    error$: Observable<string | null> = this._error.asObservable();

    constructor(private api: ApiService) { }

    loadDashboardData(): void {
        this._loading.next(true);
        this._error.next(null);

        forkJoin({
            team: this.api.getMyTeam().pipe(
                catchError(err => {
                    console.error('Error fetching team:', err);
                    return of(null);
                })
            ),
            stats: this.api.getDashboardStats().pipe(
                catchError(err => {
                    console.error('Error fetching stats:', err);
                    return of(null);
                })
            )
        }).subscribe({
            next: ({ team, stats }) => {
                this._team.next(team);
                this._dashboardStats.next(stats);
                this._loading.next(false);
            },
            error: (err) => {
                this._error.next('Failed to load dashboard data. Please try again.');
                this._loading.next(false);
            }
        });
    }

    // Helper to force a refresh if needed (e.g., after a transfer)
    refresh(): void {
        this.loadDashboardData();
    }
}
