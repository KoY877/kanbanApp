@echo off
echo.
echo ========================================
echo   TEST DE REVOCATION DE TOKEN
echo ========================================
echo.

echo [1/4] LOGIN - Obtenir les tokens initiaux...
curl -X POST http://localhost:8081/auth/login ^
  -H "Content-Type: application/json" ^
  -d "{\"email\":\"demo@test.com\",\"password\":\"Demo123!\"}" ^
  -o login.json -s

echo OK
echo.

echo Tokens initiaux :
type login.json
echo.
echo.

REM Extraire les tokens
for /f "tokens=2 delims=:," %%a in ('type login.json ^| findstr /C:"refreshToken"') do set OLD_TOKEN=%%a
set OLD_TOKEN=%OLD_TOKEN:"=%
set OLD_TOKEN=%OLD_TOKEN: =%

echo [2/4] REFRESH - Utiliser le refresh token...
curl -X POST http://localhost:8081/auth/refresh ^
  -H "Content-Type: application/json" ^
  -d "{\"refreshToken\":%OLD_TOKEN%}" ^
  -o refresh.json -s

echo OK
echo.

echo Nouveaux tokens :
type refresh.json
echo.
echo.

echo [3/4] TENTATIVE - Reussir l'ancien token (devrait echouer)...
curl -X POST http://localhost:8081/auth/refresh ^
  -H "Content-Type: application/json" ^
  -d "{\"refreshToken\":%OLD_TOKEN%}" ^
  -w "\nHTTP Status: %%{http_code}\n"

echo.
echo.
echo ========================================
echo  Si Status = 401 : Token bien revoque!
echo ========================================
echo.

del login.json refresh.json 2>nul
