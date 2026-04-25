#!/bin/bash
echo "=== TEST REVOCATION TOKEN ==="
echo ""

# 1. Login
echo "[1] Login..."
OLD_TOKEN=$(curl -X POST http://localhost:8081/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"demo@test.com","password":"Demo123!"}' -s | grep -o '"refreshToken":"[^"]*"' | cut -d'"' -f4)
echo "Old Token: ${OLD_TOKEN:0:20}..."
echo ""

# 2. Refresh (révoque l'ancien)
echo "[2] Refresh (ancien token devient révoqué)..."
NEW_TOKEN=$(curl -X POST http://localhost:8081/auth/refresh \
  -H "Content-Type: application/json" \
  -d "{\"refreshToken\":\"$OLD_TOKEN\"}" -s | grep -o '"refreshToken":"[^"]*"' | cut -d'"' -f4)
echo "New Token: ${NEW_TOKEN:0:20}..."
echo ""

# 3. Tester ancien token (devrait retourner 401)
echo "[3] Test ancien token (devrait échouer avec 401)..."
HTTP_CODE=$(curl -X POST http://localhost:8081/auth/refresh \
  -H "Content-Type: application/json" \
  -d "{\"refreshToken\":\"$OLD_TOKEN\"}" \
  -s -w "%{http_code}" -o /dev/null)

if [ "$HTTP_CODE" = "401" ]; then
    echo "SUCCESS: Token révoqué (HTTP 401)"
else
    echo "ERREUR: Token encore actif (HTTP $HTTP_CODE)"
fi
