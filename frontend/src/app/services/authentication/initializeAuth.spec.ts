import { TestBed } from '@angular/core/testing';
import { BehaviorSubject, Subject } from 'rxjs';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { AuthService } from './auth-service';
import { initializeAuth } from './initializeAuth';
import { Message } from '../message';

describe('background session restoration', () => {
  let restoration: Subject<boolean>;
  let connected: BehaviorSubject<boolean>;
  let messageConnected: ReturnType<typeof vi.fn>;

  beforeEach(() => {
    restoration = new Subject<boolean>();
    connected = new BehaviorSubject(false);
    messageConnected = vi.fn();
    TestBed.configureTestingModule({
      providers: [
        { provide: AuthService, useValue: { restoreSession: () => restoration } },
        { provide: Message, useValue: { connected$: connected, messageConnected } }
      ]
    });
  });

  it('allows bootstrap to finish while the backend is still pending', () => {
    expect(TestBed.runInInjectionContext(initializeAuth)).toBeUndefined();
    expect(messageConnected).not.toHaveBeenCalled();
    restoration.next(true);
    restoration.complete();
    expect(messageConnected).toHaveBeenCalledWith(true);
  });

  it('does not replace a new login with a late restoration result', () => {
    TestBed.runInInjectionContext(initializeAuth);
    connected.next(true);
    restoration.next(false);
    expect(messageConnected).not.toHaveBeenCalled();
    expect(restoration.observed).toBe(false);
  });

  it('keeps the app signed out when no session can be restored', () => {
    TestBed.runInInjectionContext(initializeAuth);
    restoration.next(false);
    restoration.complete();
    expect(messageConnected).toHaveBeenCalledWith(false);
  });
});
