import { TestBed } from '@angular/core/testing';
import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import { environement } from '../environements/environements';
import { Loading } from '../services/loading';
import { loadingInterceptor } from './loading.interceptor';

describe('API loading indicator', () => {
  let http: HttpClient;
  let requests: HttpTestingController;
  let loading: Loading;

  beforeEach(() => {
    // Construire un client HTTP avec le vrai intercepteur, mais un serveur simulé :
    // aucune requête réseau réelle n'est envoyée pendant ces tests.
    TestBed.configureTestingModule({ providers: [
      provideHttpClient(withInterceptors([loadingInterceptor])),
      provideHttpClientTesting()
    ] });
    // Le client lance les appels ; le contrôleur permet d'inspecter les requêtes
    // et de décider quand elles reçoivent une réponse avec flush().
    http = TestBed.inject(HttpClient);
    requests = TestBed.inject(HttpTestingController);
    loading = TestBed.inject(Loading);
  });

  // Vérifier qu'aucune requête inattendue n'est restée sans traitement.
  afterEach(() => requests.verify());

  it('stays visible until all concurrent API calls complete', () => {
    // Démarrer deux appels avant de répondre à l'un d'eux.
    http.get(`${environement.apiUrl}/boards/all`).subscribe();
    http.get(`${environement.apiUrl}/tasks`).subscribe();
    expect(loading.isLoading()).toBe(true);
    requests.expectOne(`${environement.apiUrl}/boards/all`).flush([]);
    // Le premier appel est terminé, mais celui des tâches est toujours actif.
    expect(loading.isLoading()).toBe(true);
    requests.expectOne(`${environement.apiUrl}/tasks`).flush([]);
    // Seule la fin du dernier appel doit masquer le spinner.
    expect(loading.isLoading()).toBe(false);
  });

  it('stops after an error or a cancelled request', () => {
    // Simuler une panne du serveur et vérifier que finalize libère le compteur.
    http.get(`${environement.apiUrl}/tasks`).subscribe({ error: () => {} });
    requests.expectOne(`${environement.apiUrl}/tasks`).flush({}, { status: 503, statusText: 'Unavailable' });
    expect(loading.isLoading()).toBe(false);
    // Simuler ensuite une annulation, par exemple lors d'un changement de vue.
    const subscription = http.get(`${environement.apiUrl}/boards/all`).subscribe();
    const request = requests.expectOne(`${environement.apiUrl}/boards/all`);
    subscription.unsubscribe();
    expect(request.cancelled).toBe(true);
    expect(loading.isLoading()).toBe(false);
  });

  it('ignores requests to assets and other services', () => {
    // Un fichier local ne correspond pas à l'URL de l'API : aucun spinner attendu.
    http.get('/assets/example.json').subscribe();
    expect(loading.isLoading()).toBe(false);
    requests.expectOne('/assets/example.json').flush({});
  });
});
