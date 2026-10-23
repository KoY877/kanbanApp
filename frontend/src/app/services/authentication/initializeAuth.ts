import { inject } from '@angular/core';
import { filter, takeUntil, timeout } from 'rxjs';
import { AuthService } from './auth-service';
import { Message } from '../message';

/**
 * Vérifier le cookie de session en arrière-plan, sans bloquer l'affichage.
 * Ne retourner ni Promise ni Observable : Angular ne doit pas attendre l'API
 * avant de rendre la page. Les composants réagissent via le service Message.
 */
export const initializeAuth = () => {
  const authService = inject(AuthService);
  const messageService = inject(Message);

  authService.restoreSession().pipe(
    // Éviter de conserver une vérification sans réponse au-delà de 60 secondes.
    timeout(60_000),
    // Annuler si une connexion réussit entre-temps : une ancienne réponse ne
    // doit pas effacer les tokens ou remplacer l'état de cette nouvelle session.
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
