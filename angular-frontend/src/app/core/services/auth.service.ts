import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, tap } from 'rxjs';

export interface LoginRequest { emailOrUsername: string; password: string; }
export interface RegisterRequest { username: string; email: string; password: string; }
export interface UserResponse { id: number; username: string; email: string; profileImage?: string | null; createdAt?: string; }
export interface AuthResponse { token: string; type: string; user: UserResponse; }

@Injectable({ providedIn: 'root' })
export class AuthService {
    private readonly API = 'http://localhost:8081/api/auth';

    constructor(private http: HttpClient, private router: Router) { }

    login(payload: LoginRequest): Observable<AuthResponse> {
        return this.http.post<AuthResponse>(`${this.API}/login`, payload).pipe(
            tap(res => this.persist(res))
        );
    }

    register(payload: RegisterRequest): Observable<AuthResponse> {
        return this.http.post<AuthResponse>(`${this.API}/register`, payload).pipe(
            tap(res => this.persist(res))
        );
    }

    logout(): void {
        if (typeof window !== 'undefined' && window.localStorage) {
            localStorage.removeItem('token');
            localStorage.removeItem('user');
        }
        this.router.navigate(['/']);
    }

    isAuthenticated(): boolean {
        if (typeof window !== 'undefined' && window.localStorage) {
            return !!localStorage.getItem('token');
        }
        return false;
    }

    getToken(): string | null {
        if (typeof window !== 'undefined' && window.localStorage) {
            return localStorage.getItem('token');
        }
        return null;
    }

    getUser(): UserResponse | null {
        if (typeof window !== 'undefined' && window.localStorage) {
            const raw = localStorage.getItem('user');
            try { return raw ? JSON.parse(raw) : null; } catch { return null; }
        }
        return null;
    }

    updateStoredUser(patch: Partial<UserResponse>): void {
        if (typeof window === 'undefined' || !window.localStorage) {
            return;
        }
        const current = this.getUser();
        if (!current) {
            return;
        }
        localStorage.setItem('user', JSON.stringify({ ...current, ...patch }));
    }

    setToken(token: string): void {
        if (typeof window !== 'undefined' && window.localStorage) {
            localStorage.setItem('token', token);
        }
    }

    private persist(res: AuthResponse): void {
        if (typeof window !== 'undefined' && window.localStorage) {
            localStorage.setItem('token', res.token);
            localStorage.setItem('user', JSON.stringify(res.user));
        }
    }
}
