import { computed, Injectable, signal } from '@angular/core';

// Une seule instance partagée par les intercepteurs et les composants de l'application.
@Injectable({ providedIn: 'root' })
export class Loading {
  // Un compteur permet de suivre plusieurs requêtes simultanées : un simple
  // booléen masquerait le spinner dès la fin de la première requête.
  private readonly pending = signal(0);
  // Signal dérivé, lu dans le template : Angular actualise la vue quand il change.
  readonly isLoading = computed(() => this.pending() > 0);

  start(): void {
    // Enregistrer le début d'une requête API.
    this.pending.update(count => count + 1);
  }

  finish(): void {
    // Retirer la requête terminée, sans laisser le compteur devenir négatif.
    this.pending.update(count => Math.max(0, count - 1));
  }
}
