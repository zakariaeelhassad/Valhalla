import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { ApiService } from '../services/api.service';
import { map, catchError } from 'rxjs/operators';
import { of } from 'rxjs';

export const teamCompleteGuard: CanActivateFn = (_route, _state) => {
    const apiService = inject(ApiService);
    const router = inject(Router);

    return apiService.getMyTeam().pipe(
        map(team => {
            if (team.playerCount >= 15) {
                return true;
            }
            return router.createUrlTree(['/team-selection']);
        }),
        catchError(() => {
            // If team fetch fails (e.g. no team initialized yet or error), redirect to team selection
            return of(router.createUrlTree(['/team-selection']));
        })
    );
};
