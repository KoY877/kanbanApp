# TEST DE RÉVOCATION DU REFRESH TOKEN

## Test Manuel avec Postman

### Étape 1: LOGIN
```
POST http://localhost:8081/auth/login
Content-Type: application/json

{
  "email": "demo@test.com",
  "password": "Demo123!"
}
```
**Copier le `refreshToken` de la réponse** (appelons-le TOKEN_A)

---

### Étape 2: REFRESH (première fois)
```
POST http://localhost:8081/auth/refresh
Content-Type: application/json

{
  "refreshToken": "TOKEN_A"
}
```
**Résultat attendu:** ✅ HTTP 200 OK  
**Nouveau token retourné** (appelons-le TOKEN_B)  
**IMP important:** TOKEN_A est maintenant RÉVOQUÉ dans la base de données

---

### Étape 3: TESTER L'ANCIEN TOKEN (devrait échouer)
```
POST http://localhost:8081/auth/refresh
Content-Type: application/json

{
  "refreshToken": "TOKEN_A"
}
```
**Résultat attendu:** ❌ HTTP 401 UNAUTHORIZED  
**Message:** "Refresh token has been revoked"

---

## Vérification dans les Logs

Dans les logs de l'application, vous devriez voir:
```sql
-- Lors du REFRESH (Étape 2):
Hibernate: insert into refresh_tokens (...) values (...) -- TOKEN_B créé
Hibernate: update refresh_tokens set revoked=?,revoked_at=? where id=? -- TOKEN_A révoqué
```

---

## Ce qui se passe en interne:

1. **Login:** Crée TOKEN_A (revoked=false)
2. **Refresh avec TOKEN_A:** 
   - Vérifie que TOKEN_A n'est pas révoqué ✓
   - Crée TOKEN_B (revoked=false)
   - Marque TOKEN_A comme révoqué (revoked=true, revoked_at=NOW())
3. **Réutiliser TOKEN_A:**
   - Trouve TOKEN_A dans la base
   - Vérifie le statut → revoked=true
   - **REJETTE** avec HTTP 401

---

## Code responsable de la vérification

Dans `RefreshTokenService.java`:
```java
public RefreshToken verifyExpiration(RefreshToken token) {
    // Vérification de révocation
    if (token.isRevoked()) {
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, 
            "Refresh token has been revoked");
    }
    // ... reste du code
}
```

---

## Sécurité: Détection de réutilisation

Si un attaquant essaie de réutiliser TOKEN_A après qu'il soit révoqué,  
TOUS les tokens de la même famille sont révoqués pour sécurité.

Code dans `rotateRefreshToken()`:
```java
if (oldToken.isRevoked()) {
    System.err.println("🚨 TOKEN REUSE DETECTED!");
    revokeTokenFamily(oldToken.getTokenFamily());
    throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
        "Token reuse detected - all tokens revoked");
}
```
