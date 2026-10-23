import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { defer, finalize } from 'rxjs';
import { environement } from '../environements/environements';
import { Loading } from '../services/loading';

export const loadingInterceptor: HttpInterceptorFn = (request, next) => {
  // Ne suivre que notre API : les images et les services externes ne doivent
  // pas déclencher l'indicateur de chargement des données.
  if (!request.url.startsWith(`${environement.apiUrl}/`)) {
    return next(request);
  }

  const loading = inject(Loading);
  // defer démarre le comptage à la souscription réelle, et non à la création
  // de l'Observable. Chaque nouvelle souscription a ainsi son propre comptage.
  return defer(() => {
    loading.start();
    // finalize s'exécute après un succès, une erreur ou une annulation :
    // le spinner ne reste donc pas actif à cause d'une requête interrompue.
    return next(request).pipe(finalize(() => loading.finish()));
  });
};
