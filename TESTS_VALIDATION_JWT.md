# 🧪 Tests de Validation - Migration JWT Sécurisée

## 🎯 Objectif
Valider que la migration vers httpOnly cookies + accessToken en mémoire fonctionne correctement.

---

## 🚀 Démarrage

### 1. Backend (Spring Boot)
```bash
cd todoapp_spring_boot
./mvnw clean spring-boot:run
```

**Attendu** :
```
Started KanbanappApplication in X.XXX seconds
🔍 DEBUG - Access token expiration: 900000 ms (15 minutes)
```

### 2. Frontend (Angular)
```bash
cd TodoApp_Angular
npm start
```

**Attendu** :
```
Angular Live Development Server is listening on localhost:4200
Compiled successfully.
```

---

## ✅ Test 1 : Login Initial

### Instructions
1. Ouvrir `http://localhost:4200`
2. Ouvrir **DevTools** (F12)
3. Aller dans **Application** → **Cookies** → `http://localhost:4200`
4. Se connecter avec un compte valide

### Vérifications ✅

#### Console (DevTools → Console)
```
✅ Connexion réussie - Tokens sécurisés
🔐 AccessToken stocké en MÉMOIRE
🍪 RefreshToken stocké en httpOnly cookie (inaccessible en JS)
📦 User data: { userId: "...", username: "...", email: "..." }
```

#### Cookies (DevTools → Application → Cookies)
| Name | Value | HttpOnly | Secure | SameSite | Path |
|------|-------|----------|--------|----------|------|
| `refreshToken` | `uuid...` | ✅ **Yes** | ❌ No | Lax | /auth |

#### Local Storage (DevTools → Application → Local Storage)
**NE DOIT PAS CONTENIR :**
- ❌ `accessToken`
- ❌ `refreshToken`

**DOIT CONTENIR :**
- ✅ `User-Id`
- ✅ `UserName`
- ✅ `UserEmail`
- ✅ `Role`
- ✅ `connected`

#### Network (DevTools → Network → Headers)
**Requête POST /auth/login → Response Headers**
```
Set-Cookie: refreshToken=uuid...; Path=/auth; HttpOnly; SameSite=Lax; Max-Age=604800
```

---

## ✅ Test 2 : Requêtes Authentifiées

### Instructions
1. Après le login, naviguer vers les **Boards**
2. Observer la console et l'onglet Network

### Vérifications ✅

#### Network (DevTools → Network → GET /api/boards)
**Request Headers**
```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
Cookie: refreshToken=uuid...
```

#### Console
```
🔐 Requête authentifiée: GET http://localhost:8081/api/boards
```

#### Response
```
200 OK
```

---

## ✅ Test 3 : Refresh Automatique

### Option A : Test Rapide (1 minute)

1. **Modifier** `application.properties` :
   ```properties
   jwt.access-token.expiration=60000  # 1 minute
   ```

2. **Redémarrer le backend** :
   ```bash
   ./mvnw spring-boot:run
   ```

3. **Se reconnecter**

4. **Attendre 1 minute**

5. **Faire une requête API** (naviguer, rafraîchir une liste)

### Option B : Test Normal (15 minutes)

1. Se connecter
2. Attendre 15 minutes
3. Faire une requête API

### Vérifications ✅

#### Console (au moment du refresh)
```
❌ ERREUR 401 détectée sur: GET http://localhost:8081/api/boards
🔄 Tentative de refresh du token...
🔄 Appel de l'endpoint /auth/refresh...
🍪 Le refreshToken sera envoyé automatiquement via cookie httpOnly
✅ Réponse 200 OK du refresh reçue
📦 Réponse du refresh: { hasAccessToken: true, ... }
✅ Token refreshed automatically - Session extended
🔄 Nouveau token (10 premiers caractères): eyJhbGciOi...
🔄 Retry de la requête avec le nouveau token: GET http://localhost:8081/api/boards
```

#### Network (POST /auth/refresh)
**Request**
```
Cookie: refreshToken=uuid-old...
```

**Response**
```
200 OK
Set-Cookie: refreshToken=uuid-new...; Path=/auth; HttpOnly; SameSite=Lax
```

**Body**
```json
{
  "accessToken": "eyJhbGciOi...",
  "refreshToken": null,  // ✅ null car dans cookie
  "userId": "...",
  "username": "...",
  "email": "..."
}
```

---

## ✅ Test 4 : Rafraîchissement de Page

### Instructions
1. Être connecté
2. Appuyer sur **F5** (rafraîchir la page)
3. Naviguer vers les Boards

### Vérifications ✅

#### Comportement Attendu
1. La page se recharge
2. L'utilisateur semble **déconnecté** (accessToken perdu en mémoire)
3. Au premier clic sur une ressource protégée → **401**
4. L'intercepteur appelle automatiquement `/auth/refresh`
5. L'utilisateur est **re-authentifié automatiquement**

#### Console
```
🔄 Tentative de refresh du token...
✅ Token refreshed automatically - Session extended
```

---

## ✅ Test 5 : Sécurité XSS

### Instructions
1. Être connecté
2. Ouvrir la **Console DevTools**
3. Taper ces commandes :

```javascript
// Test 1 : Tenter de lire le refreshToken depuis localStorage
localStorage.getItem('refreshToken')
// ✅ Résultat attendu : null

// Test 2 : Tenter de lire le refreshToken depuis localStorage
localStorage.getItem('accessToken')
// ✅ Résultat attendu : null

// Test 3 : Tenter de lire les cookies
document.cookie
// ✅ Résultat attendu : Le refreshToken n'apparaît PAS (httpOnly)

// Test 4 : Tenter d'accéder au TokenService (ne devrait pas fonctionner)
angular.element(document.body).injector.get('TokenService')
// ❌ Erreur attendue (normal, pas accessible globalement)
```

### Vérifications ✅
- ❌ Impossible de voler le `refreshToken` via JavaScript
- ✅ Protection XSS активée

---

## ✅ Test 6 : Rotation des Tokens

### Instructions
1. Être connecté
2. Ouvrir DevTools → Application → Cookies
3. **Noter** la valeur du cookie `refreshToken` (uuid-1)
4. Attendre l'expiration du token (1 min ou 15 min)
5. Faire une requête API
6. **Noter** la nouvelle valeur du cookie `refreshToken` (uuid-2)

### Vérifications ✅
- ✅ `uuid-1 ≠ uuid-2` (le token a été rotaté)
- ✅ Nouveau cookie avec nouvelle expiration (7 jours)

---

## ✅ Test 7 : Logout

### Instructions
1. Être connecté
2. Se déconnecter (bouton logout)

### Vérifications ✅

#### Console
```
🗑️ All tokens and user data cleared
```

#### Cookies
- ❌ Le cookie `refreshToken` **devrait être supprimé** (à implémenter si pas encore fait)

#### Local Storage
- ❌ `User-Id` supprimé
- ❌ `UserName` supprimé
- ❌ `connected` supprimé

---

## ✅ Test 8 : Token Reuse Detection

### ⚠️ Test Avancé (Simulation d'Attaque)

**Prérequis** : Accès à la base de données

### Instructions
1. Se connecter
2. Copier la valeur du cookie `refreshToken` (uuid-1) depuis DevTools
3. Attendre l'expiration du token (ou forcer un refresh)
4. Le système génère un nouveau token (uuid-2)
5. **Tenter de réutiliser** l'ancien token (uuid-1) :
   ```bash
   curl -X POST http://localhost:8081/auth/refresh \
     -H "Cookie: refreshToken=uuid-1"
   ```

### Vérifications ✅

#### Console Backend
```
⚠️ TOKEN REUSE DETECTED! Revoking entire token family: family-id
🔒 Revoked all tokens in family: family-id
```

#### Response
```
401 Unauthorized
{
  "error": "Token reuse detected - all tokens in family have been revoked"
}
```

#### Conséquence
- ✅ L'utilisateur est **déconnecté** (tous les tokens de la famille révoqués)
- ✅ Protection contre le vol de token

---

## ✅ Test 9 : CORS (Cross-Origin)

### Instructions
1. Backend sur `localhost:8081`
2. Frontend sur `localhost:4200`
3. Vérifier que les requêtes cross-origin fonctionnent

### Vérifications ✅

#### Network (Preflight OPTIONS)
```
Request Method: OPTIONS
Access-Control-Allow-Origin: http://localhost:4200
Access-Control-Allow-Credentials: true
Access-Control-Allow-Methods: GET, POST, PUT, DELETE, PATCH, OPTIONS
```

#### Network (Requête POST /auth/login)
```
Access-Control-Allow-Credentials: true
Set-Cookie: refreshToken=...
```

---

## ❌ Cas d'Erreur à Tester

### Erreur 1 : Cookie non envoyé
**Cause** : `withCredentials: false`

**Test** : Retirer temporairement `withCredentials: true` dans `auth-service.ts`

**Résultat attendu** :
```
401 Unauthorized
No refresh token provided
```

### Erreur 2 : Token expiré (7 jours)
**Test** : Modifier l'expiration à 10 secondes
```properties
jwt.refresh-token.expiration=10000  # 10 secondes
```

**Résultat attendu** :
```
401 Unauthorized
Refresh token expired. Please login again.
```

### Erreur 3 : CORS bloqué
**Test** : Modifier `cors.allowed-origins` pour exclure `localhost:4200`

**Résultat attendu** :
```
CORS error
Access to XMLHttpRequest blocked by CORS policy
```

---

## 📊 Checklist Complète

| Test | Description | Status |
|------|-------------|--------|
| 1 | Login initial avec cookie httpOnly | ⬜ |
| 2 | Requêtes authentifiées (Bearer token) | ⬜ |
| 3 | Refresh automatique (token expiré) | ⬜ |
| 4 | Rafraîchissement de page (F5) | ⬜ |
| 5 | Sécurité XSS (cookie inaccessible) | ⬜ |
| 6 | Rotation des tokens | ⬜ |
| 7 | Logout (nettoyage) | ⬜ |
| 8 | Token Reuse Detection | ⬜ |
| 9 | CORS cross-origin | ⬜ |

---

## 🎉 Validation Finale

**La migration est réussie si :**

✅ Login fonctionne + cookie httpOnly visible  
✅ Navigation fonctionne sans erreur  
✅ Refresh automatique après 15 minutes  
✅ localStorage ne contient PLUS les tokens  
✅ `document.cookie` ne montre PAS le refreshToken  
✅ Rotation des tokens fonctionne  
✅ Token reuse détecté et bloqué  

---

**Bonne chance avec les tests ! 🚀**
