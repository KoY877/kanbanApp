# 📊 Analyse du Flow JWT - TodoApp (Angular + Spring Boot)

## 🎯 Architecture Globale

Votre application utilise une **authentification JWT stateless** avec **rotation des refresh tokens** et **protection contre la réutilisation** des tokens.

---

## 📋 Vue d'Ensemble du Flow

```
┌─────────────────┐                    ┌──────────────────┐
│   Angular App   │                    │  Spring Boot API │
│  (Port 4200)    │                    │   (Port 8081)    │
└────────┬────────┘                    └────────┬─────────┘
         │                                      │
         │  1️⃣ POST /auth/login                │
         │  { email, password }                 │
         ├─────────────────────────────────────►│
         │                                      │ JwtService.generateAccessToken()
         │                                      │ RefreshTokenService.createRefreshToken()
         │                                      │
         │  2️⃣ AuthResponse                     │
         │  { accessToken, refreshToken, ... }  │
         │◄─────────────────────────────────────┤
         │                                      │
         │  💾 localStorage.setItem()           │
         │                                      │
         │  3️⃣ GET /api/boards                  │
         │  Header: Bearer {accessToken}        │
         ├─────────────────────────────────────►│
         │                                      │ JwtAuthenticationFilter
         │                                      │   ↓ validateToken()
         │                                      │   ↓ Check blacklist
         │                                      │   ↓ Extract roles
         │                                      │
         │  4️⃣ 200 OK { boards: [...] }         │
         │◄─────────────────────────────────────┤
         │                                      │
         │  ⏰ Token expired (after 15 min)     │
         │                                      │
         │  5️⃣ GET /api/boards (token expired)  │
         ├─────────────────────────────────────►│
         │                                      │
         │  6️⃣ 401 Unauthorized                 │
         │◄─────────────────────────────────────┤
         │                                      │
         │  authInterceptor détecte 401         │
         │                                      │
         │  7️⃣ POST /auth/refresh               │
         │  { refreshToken }                    │
         ├─────────────────────────────────────►│
         │                                      │ RefreshTokenService.rotateRefreshToken()
         │                                      │   ↓ Mark old token as revoked
         │                                      │   ↓ Generate new token (same family)
         │                                      │ JwtService.generateAccessToken()
         │                                      │
         │  8️⃣ AuthResponse (new tokens)        │
         │  { accessToken, refreshToken }       │
         │◄─────────────────────────────────────┤
         │                                      │
         │  💾 Update localStorage              │
         │                                      │
         │  9️⃣ Retry GET /api/boards            │
         │  Header: Bearer {NEW accessToken}    │
         │  Header: X-Retry-Request: true       │
         ├─────────────────────────────────────►│
         │                                      │
         │  🔟 200 OK { boards: [...] }         │
         │◄─────────────────────────────────────┤
```

---

## 🔐 1. Configuration JWT (Spring Boot)

### 📄 application.properties

```properties
jwt.secret=${JWT_SECRET}
jwt.access-token.expiration=900000          # 15 minutes
jwt.refresh-token.expiration=604800000      # 7 jours
```

### 🔑 JwtService.java

**Génération du Access Token:**
```java
public String generateAccessToken(User user) {
    Map<String, Object> claims = new HashMap<>();
    claims.put("email", user.getEmail());
    claims.put("userId", user.getId());
    claims.put("roles", List.of(user.getRole().name()));
    claims.put("token_type", "ACCESS");
    
    String jti = UUID.randomUUID().toString();
    
    return Jwts.builder()
        .setClaims(claims)
        .setSubject(user.getEmail())
        .setId(jti)
        .setIssuedAt(new Date(System.currentTimeMillis()))
        .setExpiration(new Date(System.currentTimeMillis() + 900000)) // 15 min
        .signWith(getSigningKey(), SignatureAlgorithm.HS256)
        .compact();
}
```

**Validation du Token:**
```java
public boolean validateToken(String token, String userEmail) {
    try {
        final String subject = extractSubject(token);
        return (subject.equals(userEmail) && !isTokenExpired(token));
    } catch (ExpiredJwtException | MalformedJwtException | SignatureException e) {
        return false;
    }
}
```

---

## 🔄 2. Système de Refresh Token Rotation

### 🛡️ Protection contre la réutilisation (Token Reuse Detection)

Votre implémentation utilise le système **Token Family** :

```java
@Transactional
public RefreshToken rotateRefreshToken(RefreshToken oldToken) {
    // 🚨 DÉTECTION D'ATTAQUE
    if (oldToken.isRevoked()) {
        System.err.println("⚠️ TOKEN REUSE DETECTED!");
        revokeTokenFamily(oldToken.getTokenFamily());
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
            "Token reuse detected - all tokens revoked");
    }
    
    // ✅ Marquer l'ancien token comme révoqué
    oldToken.setRevoked(true);
    oldToken.setRevokedAt(Instant.now());
    refreshTokenRepository.save(oldToken);
    
    // ✅ Créer un NOUVEAU token dans la MÊME famille
    RefreshToken newToken = new RefreshToken();
    newToken.setToken(UUID.randomUUID().toString());
    newToken.setTokenFamily(oldToken.getTokenFamily()); // Même famille!
    newToken.setExpiryDate(Instant.now().plusMillis(refreshTokenDuration));
    
    return refreshTokenRepository.save(newToken);
}
```

**Protection:**
- ✅ Chaque token n'est utilisable qu'**une seule fois**
- ✅ Si un token révoqué est réutilisé → **toute la famille est révoquée**
- ✅ Protection contre les attaques par **vol de token**

---

## 🔐 3. Filtre d'Authentification (JwtAuthenticationFilter)

```java
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) {
        
        String authHeader = request.getHeader("Authorization");
        
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }
        
        String token = authHeader.substring(7);
        String email = jwtService.extractEmail(token);
        
        // 🚨 VÉRIFICATION BLACKLIST
        if (tokenBlacklistService.isBlacklisted(token)) {
            logger.warn("Token is blacklisted");
            filterChain.doFilter(request, response);
            return;
        }
        
        // ✅ VALIDATION DU TOKEN
        if (jwtService.validateToken(token, email)) {
            List<String> roles = jwtService.extractClaim(token, 
                claims -> claims.get("roles", List.class));
            
            List<SimpleGrantedAuthority> authorities = roles.stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .collect(Collectors.toList());
            
            UsernamePasswordAuthenticationToken authToken = 
                new UsernamePasswordAuthenticationToken(email, null, authorities);
            
            SecurityContextHolder.getContext().setAuthentication(authToken);
        }
        
        filterChain.doFilter(request, response);
    }
}
```

**Endpoints publics (non filtrés):**
```java
protected boolean shouldNotFilter(HttpServletRequest request) {
    String path = request.getRequestURI();
    return path.startsWith("/auth/") ||
           path.startsWith("/swagger-ui") ||
           path.startsWith("/v3/api-docs");
}
```

---

## 🌐 4. Configuration CORS

### SecurityConfig.java

```java
@Bean
public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();
    
    configuration.setAllowedOriginPatterns(
        Arrays.asList(corsProperties.getAllowedOrigins().split(","))
    );
    
    configuration.setAllowedMethods(
        Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
    );
    
    configuration.setAllowedHeaders(Arrays.asList("*"));
    
    configuration.setExposedHeaders(Arrays.asList(
        "Authorization",
        "Access-Control-Allow-Origin",
        "Access-Control-Allow-Credentials"
    ));
    
    configuration.setAllowCredentials(true);
    configuration.setMaxAge(3600L);
    
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
}
```

---

## 🅰️ 5. Intercepteur Angular (auth.interceptors.ts)

### Fonctionnalités Principales

1. **Ajout automatique du JWT** sur toutes les requêtes authentifiées
2. **Détection des erreurs 401** (token expiré)
3. **Refresh automatique** du token
4. **Retry de la requête** avec le nouveau token
5. **Protection contre les boucles infinies**

```typescript
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);
  const messageService = inject(Message);
  
  const token = localStorage.getItem('accessToken');
  const publicEndpoints = ['/auth/login', '/auth/register', '/auth/refresh'];
  const isPublicEndpoint = publicEndpoints.some(endpoint => req.url.includes(endpoint));
  
  // ✅ Vérifier si c'est une requête retry
  const isRetryRequest = req.headers.has('X-Retry-Request');
  
  // ⚪ Endpoints publics → pas de token
  if (isPublicEndpoint || !token) {
    return next(req);
  }
  
  // 🔐 Ajouter le JWT dans le header
  const clonedRequest = req.clone({
    setHeaders: { Authorization: `Bearer ${token}` }
  });
  
  return next(clonedRequest).pipe(
    catchError((error: HttpErrorResponse) => {
      if (error.status === 401 && !req.url.includes('/auth/refresh')) {
        
        // 🚨 Si retry échoue → déconnexion
        if (isRetryRequest) {
          console.error('❌ Retry failed - backend rejecting refreshed token');
          localStorage.clear();
          messageService.messageConnected(false);
          return throwError(() => error);
        }
        
        // 🔄 REFRESH TOKEN FLOW
        if (!isRefreshing) {
          isRefreshing = true;
          refreshTokenSubject.next(null);
          
          const refreshToken = localStorage.getItem('refreshToken');
          if (!refreshToken) {
            isRefreshing = false;
            localStorage.clear();
            messageService.messageConnected(false);
            return throwError(() => error);
          }
          
          return authService.refreshAccessToken().pipe(
            switchMap((response) => {
              isRefreshing = false;
              
              // 💾 Sauvegarder les NOUVEAUX tokens
              localStorage.setItem('accessToken', response.accessToken);
              localStorage.setItem('refreshToken', response.refreshToken);
              
              refreshTokenSubject.next(response.accessToken);
              
              // 🔄 Retry avec le nouveau token
              const retryRequest = req.clone({
                setHeaders: {
                  'Authorization': `Bearer ${response.accessToken}`,
                  'X-Retry-Request': 'true'  // Marquer comme retry
                }
              });
              
              return next(retryRequest);
            }),
            catchError((refreshError) => {
              // ❌ Refresh échoué → déconnexion
              isRefreshing = false;
              refreshTokenSubject.next(null);
              localStorage.clear();
              messageService.messageConnected(false);
              return throwError(() => refreshError);
            })
          );
        } else {
          // ⏳ Un refresh est déjà en cours → attendre
          return refreshTokenSubject.pipe(
            filter(token => token !== null),
            take(1),
            switchMap(token => {
              const retryRequest = req.clone({
                setHeaders: { Authorization: `Bearer ${token}` }
              });
              return next(retryRequest);
            })
          );
        }
      }
      return throwError(() => error);
    })
  );
};
```

---

## 🔐 6. Service d'Authentification Angular

### AuthService.ts

```typescript
refreshAccessToken(): Observable<{ accessToken: string; refreshToken: string }> {
  const refreshToken = localStorage.getItem('refreshToken');
  if (!refreshToken) {
    throw new Error('No refresh token available');
  }
  
  return this.http.post<{ accessToken: string; refreshToken: string }>(
    `${this.apiUrl}/auth/refresh`,
    { refreshToken }
  ).pipe(
    map(response => {
      // 💾 Sauvegarder les NOUVEAUX tokens
      localStorage.setItem('accessToken', response.accessToken);
      localStorage.setItem('refreshToken', response.refreshToken);
      
      return response;
    }),
    catchError((err: any) => {
      if (err.status === 401) {
        localStorage.clear();
      }
      throw err;
    })
  );
}
```

---

## 📊 7. Cycle de Vie des Tokens

### Timeline d'une Session

```
T=0min    │ 🔐 Login
          │ ├─ accessToken (expire in 15min)
          │ └─ refreshToken (expire in 7 days)
          │
T=15min   │ ⚠️ accessToken expired
          │ 🔄 Auto-refresh triggered
          │ ├─ Old refreshToken → marked as REVOKED
          │ ├─ New accessToken (expire in 15min)
          │ └─ New refreshToken (expire in 7 days)
          │
T=30min   │ ⚠️ accessToken expired again
          │ 🔄 Auto-refresh triggered
          │ ├─ Old refreshToken → marked as REVOKED
          │ ├─ New accessToken
          │ └─ New refreshToken
          │
...       │
          │
T=7days   │ ⚠️ refreshToken expired
          │ ❌ User must re-login
```

---

## 🛡️ 8. Sécurité Implémentée

### ✅ Fonctionnalités de Sécurité

| Fonctionnalité | Implémentation | Status |
|----------------|----------------|--------|
| **JWT Signing** | HMAC-SHA256 avec clé secrète | ✅ |
| **Token Expiration** | Access: 15min, Refresh: 7 jours | ✅ |
| **Token Rotation** | Nouveau refresh token à chaque refresh | ✅ |
| **Token Reuse Detection** | Via `tokenFamily` + révocation | ✅ |
| **Token Blacklist** | Pour révocation manuelle | ✅ |
| **CORS** | Configuration stricte | ✅ |
| **HTTPS** | À configurer en production | ⚠️ |
| **Refresh Token in Database** | Stockage sécurisé en MySQL | ✅ |
| **Protection XSS** | localStorage (vulnerable to XSS) | ⚠️ |

---

## ⚠️ 9. Points d'Attention

### 🚨 Vulnérabilités Potentielles

#### 1. localStorage et XSS
```typescript
// ⚠️ VULNERABLE à XSS
localStorage.setItem('accessToken', token);
```

**Solution:** Utiliser **httpOnly cookies** pour le refreshToken

```java
// Backend: Envoyer le refresh token en cookie httpOnly
ResponseCookie cookie = ResponseCookie.from("refreshToken", token)
    .httpOnly(true)
    .secure(true)  // HTTPS only
    .sameSite("Strict")
    .maxAge(7 * 24 * 60 * 60)
    .path("/auth")
    .build();
    
response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
```

#### 2. Clé Secrète JWT
```properties
# ⚠️ Doit être FORTE en production (min 256 bits)
jwt.secret=${JWT_SECRET}
```

**Génération sécurisée:**
```bash
# Linux/Mac
openssl rand -base64 64

# PowerShell
[Convert]::ToBase64String((1..64 | ForEach-Object { Get-Random -Maximum 256 }))
```

#### 3. HTTPS en Production
```properties
# ⚠️ Actuellement HTTP - passer en HTTPS en production
server.ssl.enabled=true
server.ssl.key-store=classpath:keystore.p12
server.ssl.key-store-password=${SSL_PASSWORD}
```

---

## 📝 10. Endpoints d'Authentification

| Endpoint | Method | Auth Required | Description |
|----------|--------|---------------|-------------|
| `/auth/login` | POST | ❌ | Login + génération tokens |
| `/auth/register` | POST | ❌ | Inscription + génération tokens |
| `/auth/refresh` | POST | ❌ | Refresh access token |
| `/auth/logout` | POST | ✅ | Révocation refresh tokens |
| `/auth/revoke-all-tokens` | POST | ✅ | Révocation de tous les tokens |
| `/auth/change-password` | POST | ✅ | Changement de mot de passe |

---

## 🎯 11. Recommandations

### ✅ Points Forts
1. ✅ **Token Rotation** implémentée
2. ✅ **Token Reuse Detection** avec token family
3. ✅ **Auto-refresh transparent** côté Angular
4. ✅ **Blacklist system** pour révocation manuelle
5. ✅ **CORS configuré correctement**

### 🔧 Améliorations Possibles

#### Priorité HAUTE 🔴
1. **Migrer vers httpOnly cookies** pour le refresh token
   ```typescript
   // Angular: Le refresh token ne sera plus accessible en JS
   // Seulement envoyé automatiquement via cookie
   ```

2. **Utiliser une clé secrète forte**
   ```bash
   # Générer une vraie clé secrète (256 bits minimum)
   JWT_SECRET=<clé-générée-aléatoirement>
   ```

3. **Activer HTTPS en production**
   ```properties
   server.ssl.enabled=true
   ```

#### Priorité MOYENNE 🟡
4. **Ajouter rate limiting** sur `/auth/refresh`
   ```java
   @RateLimiter(name = "authRefresh", fallbackMethod = "rateLimitFallback")
   public ResponseEntity<AuthResponse> refresh(...)
   ```

5. **Logger les événements de sécurité**
   ```java
   securityAuditService.logTokenRefresh(userId, ipAddress);
   securityAuditService.logTokenReuseDetected(tokenFamily);
   ```

6. **Ajouter un système de fingerprinting**
   ```java
   // Lier le token à un device fingerprint
   claims.put("device_fingerprint", generateFingerprint(request));
   ```

#### Priorité BASSE 🟢
7. **Implémenter JWT refresh token sliding window**
8. **Ajouter des notifications de sécurité**
9. **Implémenter le remember-me**

---

## 📚 12. Ressources et Documentation

### Standards et Bonnes Pratiques
- [RFC 7519 - JWT](https://datatracker.ietf.org/doc/html/rfc7519)
- [RFC 6749 - OAuth 2.0](https://datatracker.ietf.org/doc/html/rfc6749)
- [OWASP JWT Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/JSON_Web_Token_for_Java_Cheat_Sheet.html)

### Librairies Utilisées
- **Backend:** `io.jsonwebtoken:jjwt` (Java JWT)
- **Frontend:** Angular HttpClient + RxJS

---

## 🏁 Conclusion

Votre implémentation JWT est **solide** et suit les **bonnes pratiques modernes** :
- ✅ Token rotation
- ✅ Reuse detection
- ✅ Auto-refresh transparent
- ✅ Blacklist support

**Prochaines étapes critiques :**
1. 🔴 Migrer le refresh token vers httpOnly cookies
2. 🔴 Utiliser une clé secrète forte (256+ bits)
3. 🔴 Activer HTTPS en production

---

*Document généré le 12 avril 2026*
