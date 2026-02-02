import { HttpClient, HttpErrorResponse, HttpResponse } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Router } from '@angular/router';
import { Observable, throwError } from 'rxjs';
import { tap, catchError } from 'rxjs/operators';
import { CustomerDTO } from '../models/customer-models';

interface AuthResponse {
  token: string;
  refreshToken: string;
  message?: string;
  profileIncomplete?: boolean;
  username?: string;
}

interface TwoFactorRequiredResponse {
  message: string;
  username: string;
}

interface CustomerRegisterPayload {
  userDetails: {
    username: string;
    email: string;
    password: string;
    phoneNumber?: string;
  };
  profileDetails?: {
    firstName?: string;
    lastName?: string;
    phoneNumber?: string;
    addresses?: any[];
  };
}

interface AdminRegisterPayload {
  username: string;
  email: string;
  password: string;
  phoneNumber?: string;
}

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private readonly API_URL = 'http://localhost:8081/api/auth';
  private tokenKey = 'accessToken';
  private refreshTokenKey = 'refreshToken';
  private usernameKey = 'username';
  private userIdKey = 'userId';
  private userEmailKey = 'userEmail';
  private userRolesKey = 'userRoles';

  private pending2FaUsername: string | null = null;
  private tempUsernameForMigration: string | null = null;

  private http = inject(HttpClient);
  private router = inject(Router);

  /**
   * Helper to decode JWT without external dependencies
   */
  private decodeToken(token: string): any {
    try {
      const base64Url = token.split('.')[1];
      const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
      const jsonPayload = decodeURIComponent(atob(base64).split('').map(function(c) {
        return '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2);
      }).join(''));
      return JSON.parse(jsonPayload);
    } catch (e) {
      console.error('AuthService: Error decoding JWT token', e);
      return null;
    }
  }

  /**
   * Updated Login: Handles identifier (Phone/Username), Migration, and 2FA
   */
  login(credentials: { identifier: string; password: string }): Observable<HttpResponse<any>> {
    return this.http.post<any>(`${this.API_URL}/login`, credentials, { observe: 'response' }).pipe(
      tap(response => {
        const body = response.body;

        // 1. Handle 2FA Required
        if (response.status === 202) {
          this.pending2FaUsername = body.username || body.identifier;
          console.log('AuthService: 2FA required for user:', this.pending2FaUsername);
          return;
        }

        // 2. Handle Migration (Missing phone number)
        if (body?.profileIncomplete) {
          this.tempUsernameForMigration = body.username;
          console.log('AuthService: Migration required for user:', body.username);
          return;
        }

        // 3. Handle Standard Success
        if (response.status === 200 && body.token) {
          this.processLoginSuccess(body, credentials.identifier);
        }
      }),
      catchError((error: HttpErrorResponse) => {
        console.error('AuthService: Login Error:', error);
        return throwError(() => error);
      })
    );
  }

  /**
   * Process and store successful login data
   */
  private processLoginSuccess(authResponse: AuthResponse, identifier: string) {
    this.saveToken(authResponse.token);
    this.saveRefreshToken(authResponse.refreshToken);
    
    // Store the actual username returned by server, fallback to identifier
    const storedUsername = authResponse.username || identifier;
    localStorage.setItem(this.usernameKey, storedUsername);

    const decodedToken = this.decodeToken(authResponse.token);
    if (decodedToken) {
      if (decodedToken.id) localStorage.setItem(this.userIdKey, decodedToken.id.toString());
      if (decodedToken.email) localStorage.setItem(this.userEmailKey, decodedToken.email);
      if (decodedToken.roles) localStorage.setItem(this.userRolesKey, decodedToken.roles);
    }
    
    console.log('AuthService: Login success for:', storedUsername);
  }

  verify2FACode(identifier: string, code: string): Observable<AuthResponse> {
    const payload = { identifier, twoFactorCode: code };
    return this.http.post<AuthResponse>(`${this.API_URL}/verify-2fa`, payload).pipe(
      tap(response => {
        this.processLoginSuccess(response, identifier);
        this.pending2FaUsername = null;
      }),
      catchError((error: HttpErrorResponse) => {
        console.error('AuthService: 2FA Verification Error:', error);
        return throwError(() => error);
      })
    );
  }

  linkPhoneNumber(username: string, phoneNumber: string): Observable<any> {
    return this.http.post(`${this.API_URL}/complete-profile`, { username, phoneNumber });
  }

  registerCustomer(payload: CustomerDTO): Observable<any> {
    return this.http.post(`${this.API_URL}/register`, payload);
  }

  registerAdmin(payload: AdminRegisterPayload): Observable<any> {
    return this.http.post(`${this.API_URL}/register-admin`, payload);
  }

  // --- GETTERS & TOKEN MANAGEMENT ---

  saveToken(token: string) { localStorage.setItem(this.tokenKey, token); }
  getToken(): string | null { return localStorage.getItem(this.tokenKey); }
  saveRefreshToken(token: string) { localStorage.setItem(this.refreshTokenKey, token); }
  getRefreshToken(): string | null { return localStorage.getItem(this.refreshTokenKey); }

  getCurrentUsername(): string | null {
    const username = localStorage.getItem(this.usernameKey);
    console.log('AuthService: getCurrentUsername retrieved:', username);
    return username;
  }

  getCurrentUserId(): number | null {
    const userId = localStorage.getItem(this.userIdKey);
    return userId ? +userId : null;
  }

  getCurrentUserEmail(): string | null {
    return localStorage.getItem(this.userEmailKey);
  }

  getUserRoles(): string[] {
    const rolesString = localStorage.getItem(this.userRolesKey);
    if (!rolesString) return [];
    return rolesString.split(',').map((role: string) => role.trim().replace('ROLE_', ''));
  }

  isLoggedIn(): boolean {
    const token = this.getToken();
    return !!token && !this.isTokenExpired(token);
  }

  logout() {
    localStorage.clear();
    this.pending2FaUsername = null;
    this.tempUsernameForMigration = null;
    console.log('AuthService: Logout complete.');
    this.router.navigate(['/login']);
  }

  isTokenExpired(token: string): boolean {
    const decoded = this.decodeToken(token);
    if (!decoded || !decoded.exp) return false;
    return !(decoded.exp * 1000 > Date.now());
  }

  getTempUsername(): string | null { return this.tempUsernameForMigration; }
  getPending2FaUsername(): string | null { return this.pending2FaUsername; }
  clearPending2FaUsername(): void { this.pending2FaUsername = null; }

  getUserRoleForDisplay(): string | null {
    const roles = this.getUserRoles();
    if (roles.includes('SUPER_ADMIN')) return 'SUPER_ADMIN';
    if (roles.includes('ADMIN')) return 'ADMIN';
    if (roles.includes('CUSTOMER')) return 'CUSTOMER';
    return null;
  }

  isAdmin(): boolean {
    const roles = this.getUserRoles();
    return roles.includes('ADMIN');
  }

  isSuperAdmin(): boolean {
    const roles = this.getUserRoles();
    return roles.includes('SUPER_ADMIN');
  }
  refreshToken(): Observable<AuthResponse> {
    const refreshToken = this.getRefreshToken();
    if (!refreshToken) {
      return throwError(() => new Error('No refresh token available'));
    }

    return this.http.post<AuthResponse>(`${this.API_URL}/refresh-token`, { refreshToken }).pipe(
      tap(response => {
        // Reuse login success logic to update storage with new tokens
        this.processLoginSuccess(response, this.getCurrentUsername() || '');
      }),
      catchError((error) => {
        this.logout();
        return throwError(() => error);
      })
    );
  }

}