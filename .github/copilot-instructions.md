# Copilot Instructions — TodoApp Spring Boot

## Contexte

Backend Spring Boot d'une application Kanban fullstack.
Frontend : Angular (JWT Bearer token + cookie httpOnly pour le refresh token).

---

## Règles générales

- Expliquer chaque correction (problème → cause → solution).
- Préciser le nom du fichier à modifier.
- Ne jamais modifier un fichier non concerné par le bug actuel.
- Toujours vérifier la correspondance frontend ↔ backend.
- Expliquer les corrections simplement (niveau junior).
- Toujours donner : problème / cause / correction / explication.
- Avant de toucher un **élément protégé**, afficher un avertissement explicite et demander confirmation.

---

## ⚠️ ÉLÉMENTS PROTÉGÉS — NE PAS MODIFIER SANS AVERTISSEMENT

Ces éléments ont été validés et forment le cœur du mécanisme d'authentification sécurisé.

### `AuthController.java` — endpoint `POST /auth/logout`

| Élément                                      | Raison de la protection                                                                                                       |
| -------------------------------------------- | ----------------------------------------------------------------------------------------------------------------------------- |
| `revokeAllByUserId(user.getId())`            | Révoque tous les refresh tokens en base. Sans cet appel, l'ancien utilisateur peut se reconnecter via le cookie.              |
| `ResponseCookie` avec `maxAge(0)`            | Supprime le cookie httpOnly côté navigateur. Sans cet appel, le cookie persiste et restaure la session après refresh de page. |
| `httpOnly(true)` + `path("/")` sur le cookie | Paramètres de sécurité obligatoires du cookie.                                                                                |

---

### `RefreshTokenService.java`

| Élément                                                  | Raison de la protection                                                |
| -------------------------------------------------------- | ---------------------------------------------------------------------- |
| `rotateRefreshToken()` avec `tokenFamily`                | Implémente la rotation sécurisée des tokens. Ne pas simplifier.        |
| Détection de réutilisation (grace period 30s)            | Protège contre les attaques de réutilisation de token.                 |
| Révocation de toute la `tokenFamily` si attaque détectée | Invalide toutes les sessions de l'utilisateur en cas de compromission. |
| `revokeAllByUserId(userId)`                              | Utilisé par le logout. Doit marquer tous les tokens `revoked = true`.  |

---

### `JwtAuthenticationFilter.java`

| Élément                                    | Raison de la protection                                                    |
| ------------------------------------------ | -------------------------------------------------------------------------- |
| Vérification blacklist avant validation    | Empêche l'utilisation d'access tokens révoqués même non expirés.           |
| Skip des routes `/auth/*`                  | Les routes d'auth ne doivent pas exiger de Bearer token.                   |
| Extraction des rôles depuis les claims JWT | Garantit que les droits sont portés par le token et non rechargés en base. |

---

### `SecurityConfig.java`

| Élément                                                                | Raison de la protection                                     |
| ---------------------------------------------------------------------- | ----------------------------------------------------------- |
| `SessionCreationPolicy.STATELESS`                                      | L'app est 100% JWT, pas de session serveur. Ne pas changer. |
| CSRF désactivé                                                         | Correct pour une API stateless consommée par Angular.       |
| `JwtAuthenticationFilter` avant `UsernamePasswordAuthenticationFilter` | Ordre obligatoire pour le bon fonctionnement du filtre JWT. |

---

### `TokenBlacklistService.java`

| Élément                                                    | Raison de la protection                                                       |
| ---------------------------------------------------------- | ----------------------------------------------------------------------------- |
| Blacklist via JTI (JWT ID)                                 | Mécanisme de révocation des access tokens avant expiration. Ne pas supprimer. |
| `cleanupExpiredBlacklistedTokens()` (schedulé 3h du matin) | Nettoyage automatique. Ne pas supprimer.                                      |

---

## Procédure si un élément protégé doit être modifié

1. **Afficher un avertissement** : `⚠️ Cet élément est protégé. Voici pourquoi il a été corrigé ainsi : [raison].`
2. **Expliquer pourquoi la modification est nécessaire** malgré la protection.
3. **Attendre confirmation** de l'utilisateur avant de procéder.

---

## Architecture à respecter

- **Access token** : JWT Bearer, court-vécu, transmis dans le header `Authorization`.
- **Refresh token** : UUID, long-vécu, stocké en base + envoyé via cookie `httpOnly` uniquement. Jamais dans le body de réponse.
- **Rotation** : à chaque `POST /auth/refresh`, un nouveau refresh token est émis et l'ancien est révoqué (`tokenFamily` suit la chaîne).
- **Logout** : révoque les refresh tokens en base ET supprime le cookie (`maxAge=0`).
- **Ne jamais exposer les entités directement** → utiliser les DTO (`AuthResponse`, `BoardResponse`, etc.).
- **API REST propre** : `/boards` au lieu de `/boards/all`, ressources nommées au pluriel.
- **CORS** : configuré via `CorsProperties` (fichier de config), ne pas hard-coder les origines.

## Bonnes pratiques de sécurité

- Vérifier les erreurs 401 / 403 avant de chercher ailleurs.
- Ne jamais logger un token JWT, un mot de passe, ou un cookie.
- Ne jamais retourner un stack trace Java dans la réponse HTTP → utiliser `GlobalExceptionHandler`.
- Les mots de passe doivent toujours être hashés via BCrypt avant persistance.
