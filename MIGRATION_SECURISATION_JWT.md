# 🔐 Migration Sécurisée JWT - Guide de Migration

## 📋 Résumé des Changements

### **AVANT (Vulnérable aux attaques XSS)**
```typescript
// ❌ Tokens stockés dans localStorage (accessible en JavaScript)
localStorage.setItem('accessToken', token);
localStorage.setItem('refreshToken', token);
```

### **APRÈS (Sécurisé contre XSS)** ✅
```typescript
// ✅ accessToken → Mémoire (perdu au rafraîchissement de page)
tokenService.setAccessToken(token);

// ✅ refreshToken → httpOnly cookie (inaccessible en JavaScript)
// Géré automatiquement par le backend
```

---

## 🔄 Changements Effectués

### **1. Backend (Spring Boot)**

#### ✅ AuthController.java
- **login()** : Envoie le refreshToken en httpOnly cookie
- **register()** : Envoie le refreshToken en httpOnly cookie
- **refresh()** : Lit le refreshToken depuis le cookie, retourne un nouveau en cookie

```java
// 🔐 Création du cookie httpOnly
ResponseCookie refreshTokenCookie = ResponseCookie.from("refreshToken", token)
    .httpOnly(true)      // ✅ Inaccessible en JavaScript
    .secure(false)       // ⚠️ TODO: true en PRODUCTION (HTTPS)
    .sameSite("Lax")     // ✅ Protection CSRF
    .path("/auth")       // ✅ Envoyé uniquement sur /auth/*
    .maxAge(7 * 24 * 60 * 60)  // 7 jours
    .build();

response.addHeader(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString());
```

#### ✅ SecurityConfig.java
- **CORS** : Déjà configuré avec `allowCredentials: true`

---

### **2. Frontend (Angular)**

#### ✅ Nouveau Service : token.service.ts
```typescript
@Injectable({ providedIn: 'root' })
export class TokenService {
  private accessToken: string | null = null;  // 🔒 En MÉMOIRE
  
  setAccessToken(token: string): void { ... }
  getAccessToken(): string | null { ... }
  clearAccessToken(): void { ... }
  setUserData(data): void { ... }
  clearAll(): void { ... }
}
```

#### ✅ auth.interceptors.ts
- Utilise `TokenService` au lieu de `localStorage`
- Ajoute `withCredentials: true` pour envoyer les cookies

#### ✅ auth-service.ts
- Utilise `TokenService` au lieu de `localStorage`
- `refreshAccessToken()` : Body vide + `withCredentials: true`

#### ✅ sign-in.ts
- Utilise `TokenService.setAccessToken()` au lieu de `localStorage`
- Ne reçoit plus le refreshToken dans le JSON (il est en cookie)

---

## 🧪 Guide de Test

### **1. Redémarrer le Backend**
```bash
cd todoapp_spring_boot
./mvnw spring-boot:run
```

### **2. Redémarrer Angular**
```bash
cd TodoApp_Angular
npm start
```

### **3. Tester le Flow de Login**

1. **Ouvrir les DevTools** (F12) → **Application** → **Cookies** → `http://localhost:4200`
2. **Se connecter** avec un compte valide
3. **Vérifier** :
   - ✅ Console : "🔐 AccessToken stocké en MÉMOIRE"
   - ✅ Console : "🍪 RefreshToken stocké en httpOnly cookie"
   - ✅ DevTools → Cookies : Voir `refreshToken` avec `HttpOnly ✓`
   - ✅ DevTools → Application → Local Storage : **PAS** de `accessToken` ni `refreshToken`

### **4. Tester le Refresh Automatique**

**Option A : Modifier l'expiration (test rapide)**
```properties
# application.properties
jwt.access-token.expiration=60000  # 1 minute au lieu de 15
```

**Option B : Attendre 15 minutes**

1. **Faire une requête API** (naviguer vers Boards, etc.)
2. **Attendre l'expiration** (15 min ou 1 min si modifié)
3. **Faire une nouvelle requête**
4. **Vérifier dans la console** :
   ```
   ❌ ERREUR 401 détectée sur: GET /api/boards
   🔄 Tentative de refresh du token...
   🍪 Le refreshToken sera envoyé automatiquement via cookie httpOnly
   ✅ Réponse 200 OK du refresh reçue
   ✅ Token refreshed automatically - Session extended
   🔄 Retry de la requête avec le nouveau token: GET /api/boards
   ```

### **5. Vérifier la Sécurité**

**Test XSS (Simuler une attaque)** :
1. Ouvrir la **Console DevTools**
2. Taper : `localStorage.getItem('refreshToken')`
3. **Résultat attendu** : `null` ✅
4. Taper : `document.cookie`
5. **Résultat attendu** : Le `refreshToken` **n'apparaît PAS** (HttpOnly) ✅

---

## 🔐 Sécurité

### **Protection contre XSS**

| Token | Stockage | Accessible en JS ? | Vol possible ? |
|-------|----------|-------------------|----------------|
| **accessToken** | Mémoire (variable) | ✅ Oui (temporaire) | ⚠️ Oui, mais seulement pendant la session active |
| **refreshToken** | httpOnly Cookie | ❌ **NON** | ❌ **NON** - Cookie httpOnly protégé |

### **Avantages de cette Architecture**

1. ✅ **refreshToken** : Impossible à voler via XSS
2. ✅ **accessToken** : Perdu au rafraîchissement de page (session courte)
3. ✅ **Protection CSRF** : `sameSite=Lax`
4. ✅ **Rotation des tokens** : Détection de réutilisation
5. ✅ **Token Reuse Detection** : Révocation de la famille complète

### **⚠️ Points d'Attention**

#### **1. Rafraîchissement de Page**
- L'`accessToken` est perdu au rafraîchissement
- **Solution automatique** : Au premier 401, l'intercepteur appelle `/auth/refresh` avec le cookie

#### **2. Production (HTTPS)**
Modifier dans `AuthController.java` :
```java
.secure(true)  // ⚠️ À activer en PRODUCTION
```

Vérifier `application.properties` :
```properties
server.ssl.enabled=true
server.ssl.key-store=classpath:keystore.p12
server.ssl.key-store-password=${SSL_PASSWORD}
```

#### **3. Domaine Cross-Origin**
Si frontend et backend sont sur des domaines différents :
```properties
# application.properties
cors.allowed-origins=https://app.example.com,https://www.example.com
```

Et s'assurer que le domaine du cookie correspond :
```java
.domain(".example.com")  // ✅ Fonctionne pour app.example.com et www.example.com
```

---

## 📊 Comparaison Avant/Après

### **Stockage des Tokens**

#### AVANT ❌
```
localStorage 📦
├── accessToken: "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."  ⚠️ Accessible en JS
├── refreshToken: "a3f2c8d9-1234-5678-90ab-cdef12345678"  ⚠️ Accessible en JS
├── User-Id: "abc123"
├── UserName: "john_doe"
└── connected: "true"
```

#### APRÈS ✅
```
Memory (TokenService) 🧠
├── accessToken: "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."  ✅ Perdu au refresh

Cookies (httpOnly) 🍪
└── refreshToken: "a3f2c8d9-1234-5678-90ab-cdef12345678"  ✅ Inaccessible en JS

localStorage 📦 (données non sensibles)
├── User-Id: "abc123"
├── UserName: "john_doe"
└── connected: "true"
```

---

## 🎯 Prochaines Étapes (Optionnelles)

### **1. Session Persistante (Optional)**
Si vous voulez que l'utilisateur reste connecté après fermeture du navigateur :
- Stocker un flag `rememberMe` en localStorage
- Au démarrage, appeler `/auth/refresh` automatiquement
- Implémenter une route de vérification (`/auth/me`)

### **2. Logout Côté Backend**
Ajouter un endpoint pour supprimer le cookie :
```java
@PostMapping("/logout")
public ResponseEntity<Void> logout(HttpServletResponse response) {
    // Supprimer le cookie
    ResponseCookie cookie = ResponseCookie.from("refreshToken", "")
        .httpOnly(true)
        .secure(false)
        .path("/auth")
        .maxAge(0)  // ✅ Expire immédiatement
        .build();
    
    response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    
    // Révoquer les tokens en BDD
    // ...
    
    return ResponseEntity.ok().build();
}
```

### **3. Monitoring**
Ajouter des logs de sécurité :
```java
@Service
public class SecurityAuditService {
    public void logTokenRefresh(String userId, String ipAddress) {
        // Log dans la BDD ou fichier
    }
    
    public void logFailedRefresh(String ipAddress) {
        // Détecter tentatives d'attaque
    }
}
```

---

## ✅ Checklist de Migration

- [x] Backend : `AuthController.java` modifié (login, register, refresh)
- [x] Backend : Cookies httpOnly configurés
- [x] Backend : CORS avec credentials
- [x] Frontend : `TokenService` créé
- [x] Frontend : `auth.interceptors.ts` mis à jour
- [x] Frontend : `auth-service.ts` mis à jour
- [x] Frontend : `sign-in.ts` mis à jour
- [ ] **Test** : Login fonctionne
- [ ] **Test** : Refresh automatique fonctionne
- [ ] **Test** : Cookie httpOnly visible dans DevTools
- [ ] **Test** : `localStorage` ne contient plus les tokens
- [ ] **Production** : Passer `secure: true` en HTTPS

---

## 🆘 Troubleshooting

### **Problème : Cookie non envoyé**
```
❌ No refresh token provided
```

**Solution** :
1. Vérifier `withCredentials: true` dans toutes les requêtes
2. Vérifier CORS : `allowCredentials: true`
3. Vérifier que le domaine correspond (localhost:4200 → localhost:8081)

### **Problème : Token non sauvegardé en mémoire**
```
❌ User not authenticated
```

**Solution** :
1. Vérifier que `tokenService.setAccessToken()` est appelé après login
2. Vérifier les logs : "🔐 AccessToken stocké en MÉMOIRE"

### **Problème : Refresh échoue (401)**
```
❌ ÉCHEC de l'appel /auth/refresh
Status: 401
```

**Solution** :
1. Vérifier que le cookie `refreshToken` existe dans DevTools
2. Vérifier que le backend lit correctement `request.getCookies()`
3. Vérifier que le token n'est pas expiré (7 jours max)
4. Vérifier les logs backend : "🍪 Lire le refreshToken depuis le cookie"

---

**Migration réalisée le 12 avril 2026**
