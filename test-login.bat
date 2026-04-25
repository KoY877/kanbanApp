@echo off
echo.
echo ========================================
echo    TEST DE CONNEXION - TodoApp
echo ========================================
echo.

echo [1/2] Creation utilisateur demo...
curl -X POST http://localhost:8081/auth/register ^
  -H "Content-Type: application/json" ^
  -d "{\"username\":\"demo\",\"email\":\"demo@test.com\",\"password\":\"Demo123!\"}" ^
  -s -o nul -w "%%{http_code}\n" >nul 2>&1

echo.
echo [2/2] Test LOGIN...
curl -X POST http://localhost:8081/auth/login ^
  -H "Content-Type: application/json" ^
  -d "{\"email\":\"demo@test.com\",\"password\":\"Demo123!\"}"

echo.
echo ========================================
