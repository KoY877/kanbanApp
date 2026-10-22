import { inject } from '@angular/core';
import { filter, takeUntil, timeout } from 'rxjs';
import { AuthService } from './auth-service';
import { Message } from '../message';

/**
 * Start cookie validation without making rendering wait for the backend.
 * Cancel if a new login succeeds while session restoration is pending.
 */
export const initializeAuth = () => {
  const authService = inject(AuthService);
  const messageService = inject(Message);

  authService.restoreSession().pipe(
    timeout(60_000),
    takeUntil(messageService.connected$.pipe(filter(Boolean)))
  ).subscribe({
    next: (restored) => {
      messageService.messageConnected(restored);
    },
    error: () => {
      messageService.messageConnected(false);
    }
  });
};
