import { inject, Injectable } from '@angular/core';
import { BehaviorSubject, lastValueFrom, Observable } from 'rxjs';
import { AuthService } from './auth-service';

/**
 * TokenService - Secure storage for JWT tokens
 *
 * SECURITY:
 * - accessToken: JavaScript memory (lost on page refresh)
 * - refreshToken: httpOnly cookie (backend) - inaccessible in JavaScript
 *
 * XSS PROTECTION:
 * The refreshToken cannot be stolen by a malicious script
 * The accessToken is in memory and is lost when the page is closed
 */
@Injectable({
  providedIn: 'root',
})
export class TokenService {
  // Token stored IN MEMORY (non-persistent)
  private accessToken: string | null = null;

  // Observable to notify authentication state changes
  private isAuthenticatedSubject = new BehaviorSubject<boolean>(false);
  public isAuthenticated$: Observable<boolean> = this.isAuthenticatedSubject.asObservable();

  // User data in memory
  private userData: {
    userId?: string;
    username?: string;
    email?: string;
    role?: string;
  } | null = null;

  constructor() {
    // On startup, the in-memory token is always null after a page refresh
    // (requires re-login or a token refresh via the httpOnly cookie)
  }

  /**
   * Save the access token IN MEMORY
   */
  setAccessToken(token: string): void {
    this.accessToken = token;
    this.isAuthenticatedSubject.next(true);
  }

  /**
   * Retrieve the access token from memory
   */
  getAccessToken(): string | null {
    return this.accessToken;
  }

  /**
   * Remove the access token from memory
   */
  clearAccessToken(): void {
    this.accessToken = null;
    this.isAuthenticatedSubject.next(false);
  }

  /**
   * Check if the user is authenticated
   */
  isAuthenticated(): boolean {
    return this.accessToken !== null;
  }

  /**
   * Save user data
   */
  setUserData(data: { userId: string; username: string; email: string; role: string }): void {
    this.userData = data;
    // Keep userId in localStorage for compatibility (non-sensitive)
    localStorage.setItem('User-Id', data.userId);
    localStorage.setItem('UserName', data.username);
    localStorage.setItem('UserEmail', data.email);
    localStorage.setItem('Role', data.role);
  }

  /**
   * Retrieve user data
   */
  getUserData(): typeof this.userData {
    return this.userData;
  }

  /**
   * Retrieve the user ID
   */
  getUserId(): string | null {
    return this.userData?.userId || localStorage.getItem('User-Id');
  }

  /**
   * Clear all data (logout)
   */
  clearAll(): void {
    this.accessToken = null;
    this.userData = null;
    this.isAuthenticatedSubject.next(false);

    // Clear non-sensitive localStorage data
    localStorage.removeItem('User-Id');
    localStorage.removeItem('UserName');
    localStorage.removeItem('UserEmail');
    localStorage.removeItem('Role');
    localStorage.removeItem('connected');
    localStorage.removeItem('isAuthenticated');
  }

  /**
   * Attempt to restore the session at startup.
   * Uses the refreshToken from the httpOnly cookie.
   */
  async tryRestoreSession(): Promise<boolean> {
    // If a token is already in memory, no refresh is needed
    if (this.accessToken) {
      return true;
    }

    // Check whether the user was previously connected (userId in localStorage)
    const userId = localStorage.getItem('User-Id');
    if (!userId) {
      return false; // No previously connected user
    }

    try {
      // The refresh call automatically uses the httpOnly cookie
      const response = await lastValueFrom(
        inject(AuthService).refreshAccessToken()
      );

      this.setAccessToken(response.accessToken);
      return true;
    } catch (error) {
      this.clearAll();
      return false;
    }
  }
}
