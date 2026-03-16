import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';
import { teamCompleteGuard } from './core/guards/team-complete.guard';

export const routes: Routes = [
    {
        path: '',
        loadComponent: () => import('./pages/home/home.component').then(m => m.HomeComponent),
    },
    {
        path: 'login',
        loadComponent: () => import('./pages/login/login.component').then(m => m.LoginComponent),
    },
    {
        path: 'register',
        loadComponent: () => import('./pages/register/register.component').then(m => m.RegisterComponent),
    },
    {
        path: 'dashboard',
        canActivate: [authGuard],
        loadComponent: () => import('./pages/dashboard/dashboard.component').then(m => m.DashboardComponent),
    },
    {
        path: 'points',
        canActivate: [authGuard, teamCompleteGuard],
        loadComponent: () => import('./pages/points/points.component').then(m => m.PointsComponent),
    },
    {
        path: 'team-selection',
        canActivate: [authGuard],
        loadComponent: () => import('./pages/team-selection/team-selection.component').then(m => m.TeamSelectionComponent),
    },
    { path: '**', redirectTo: '' },
];
