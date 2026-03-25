import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable, forkJoin, catchError, of } from 'rxjs';
import { ApiService } from './api.service';
import { PointsPageData, TeamResponse } from '../models';

@Injectable({
    providedIn: 'root'
})
export class PointsStateService {
    private _team = new BehaviorSubject<TeamResponse | null>(null);
    private _pointsData = new BehaviorSubject<PointsPageData | null>(null);
    private _loading = new BehaviorSubject<boolean>(true);
    private _error = new BehaviorSubject<string | null>(null);

    team$: Observable<TeamResponse | null> = this._team.asObservable();
    pointsData$: Observable<PointsPageData | null> = this._pointsData.asObservable();
    loading$: Observable<boolean> = this._loading.asObservable();
    error$: Observable<string | null> = this._error.asObservable();

    constructor(private api: ApiService) { }

    loadPointsData(gameweek?: number): void {
        this._loading.next(true);
        this._error.next(null);

        forkJoin({
            team: this.api.getMyTeam().pipe(catchError(() => of(null))),
            pointsData: this.api.getPointsPageData(gameweek).pipe(catchError(() => of(null)))
        }).subscribe({
            next: ({ team, pointsData }) => {
                this._team.next(team);
                this._pointsData.next(pointsData);
                this._loading.next(false);
            },
            error: () => {
                this._error.next('Failed to load points data.');
                this._loading.next(false);
            }
        });
    }
}
