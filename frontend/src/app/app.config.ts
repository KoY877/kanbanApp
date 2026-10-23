import { ApplicationConfig, provideAppInitializer, provideBrowserGlobalErrorListeners } from '@angular/core';
import { provideRouter } from '@angular/router';
import { authInterceptor } from './interceptors/auth.interceptors';
import { routes } from './app.routes';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { initializeAuth } from './services/authentication/initializeAuth';
import { loadingInterceptor } from './interceptors/loading.interceptor';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideRouter(routes),
    provideHttpClient(
      // Le suivi du chargement enveloppe l'authentification : il reste actif
      // pendant un éventuel renouvellement du token et la reprise de la requête.
      withInterceptors([loadingInterceptor, authInterceptor])
    ),
    provideAppInitializer(initializeAuth)
  ]
};
