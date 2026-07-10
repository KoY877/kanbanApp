import { Injectable } from '@angular/core';
import { environement } from '../environements/environement.dev';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { map, Observable, throwError } from 'rxjs';
import { Members } from '../models/members.model';
import { TokenService } from './authentication/tokenService';

@Injectable({
  providedIn: 'root',
})
export class MemberService {
   private readonly apiUrl = environement.apiUrl;

  constructor(
    private readonly http: HttpClient,
    private readonly tokenService: TokenService
  ) {}

  /**
   * Guard: throw if the user is not currently authenticated.
   * The interceptor adds the Authorization header automatically; this only
   * fails fast when there is no session at all.
   */
  private ensureAuthenticated(): void {
    if (!this.tokenService.isAuthenticated() || !this.tokenService.getUserId()) {
      throw new Error('User not authenticated. Please sign in.');
    }
  }

  /**
   * Fetch all members belonging to a given board (client-side filtered).
   * @param entity - API resource name (e.g. 'board/member')
   * @param boardId - board id to filter members by
   */
  getMembersByBoard<T extends { boardId?: string }>(entity: string, boardId?: string): Observable<T[]> {
    this.ensureAuthenticated();

    // The interceptor will automatically add the Authorization header
    return this.http.get<T[]>(`${this.apiUrl}/${entity}`).pipe(
      map(items =>
      Array.isArray(items) ? items.filter(item => item.boardId === boardId) : []
      )
    );
  }

  /**
   * Create a new member.
   * @param entity - API resource name
   * @param data - the member payload to POST
   */
 addMemberData(entity: string, data: Members): Observable<Members> {
    this.ensureAuthenticated();

    // The interceptor will automatically add the Authorization header
    return this.http.post<Members>(`${this.apiUrl}/${entity}`, data);
  }

  /**
   * Partially update an existing member (PATCH).
   * @param entity - API resource name
   * @param data - partial payload; must include an `id` field
   */
  updateMemberData<T extends {id?: string}>(entity: string, data: T): Observable<T> {
    this.ensureAuthenticated();

    // The interceptor will automatically add the Authorization header
    return this.http.patch<T>(
      `${this.apiUrl}/${entity}/${data.id}`,
      data
    );
  }

  /**
   * Delete a member by id.
   * @param entity - API resource name
   * @param data - object carrying the `id` of the member to delete
   */
  delete<T extends {id?: string}>(entity: string, data: T): Observable<T> {
    this.ensureAuthenticated();

    // The interceptor will automatically add the Authorization header
    return this.http.delete<T>(
      `${this.apiUrl}/${entity}/${data.id}`
    );
  }

  /**
   * Fetch all members of a given resource type.
   * @param entity - API resource name
   */
  getMemberData<T>(entity: string): Observable<T[]> {
    this.ensureAuthenticated();

    // The interceptor will automatically add the Authorization header
    return this.http.get<T[]>(`${this.apiUrl}/${entity}`);
  }

  /**
   * Search members by name (client-side filtering on `step1.name`).
   * @param entity - API resource name
   * @param query - search string
   */
  searchMemberData<T extends { step1: { name: string } }>(entity: string, query: string): Observable<T[]> {
    this.ensureAuthenticated();

    // The interceptor will automatically add the Authorization header
    return this.http.get<T[]>(`${this.apiUrl}/${entity}`).pipe(
      map(items => {
        if (!Array.isArray(items)) return [];
        const normalizedQuery = query.toLowerCase();
        return items.filter(item => item.step1.name.toLowerCase().includes(normalizedQuery));
      })
    );
  }

  /**
   * Search members by email (client-side filtering).
   * @param entity - API resource name
   * @param query - search string matched against memberEmail
   */
  searchDataEmail<T extends { memberEmail: string }>(entity: string, query: string): Observable<T[]> {
    this.ensureAuthenticated();

    // The interceptor will automatically add the Authorization header
    return this.http.get<T[]>(`${this.apiUrl}/${entity}`).pipe(
      map(items => {
        if (!Array.isArray(items)) return [];
        const normalizedQuery = query.toLowerCase();
        return items.filter(item => item.memberEmail.toLowerCase().includes(normalizedQuery));
      })
    );
  }
}
