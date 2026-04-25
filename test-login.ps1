# Script de test pour le login
Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "   TEST DE CONNEXION - TodoApp" -ForegroundColor Cyan
Write-Host "========================================`n" -ForegroundColor Cyan

# Test 1: Vérifier que le serveur répond
Write-Host "[1/3] Vérification du serveur..." -ForegroundColor Yellow
try {
    $healthCheck = Invoke-WebRequest -Uri "http://localhost:8081" -UseBasicParsing -ErrorAction Stop
    Write-Host "      ✓ Serveur accessible" -ForegroundColor Green
}
catch {
    if ($_.Exception.Response -and $_.Exception.Response.StatusCode.value__ -eq 401) {
        Write-Host "      ✓ Serveur accessible (401 attendu sans auth)" -ForegroundColor Green
    }
    else {
        Write-Host "      ✗ ERREUR: Serveur inaccessible" -ForegroundColor Red
        Write-Host "        Assurez-vous que l'application est démarrée" -ForegroundColor Red
        return
    }
}


# Test 2: Créer un utilisateur de test
Write-Host "`n[2/3] Création utilisateur de test..." -ForegroundColor Yellow
$registerBody = @{
    username = "testlogin"
    email = "testlogin@example.com"
    password = "TestLogin123!"
} | ConvertTo-Json

try {
    $response = Invoke-RestMethod -Uri "http://localhost:8081/auth/register" `
        -Method POST `
        -Body $registerBody `
        -ContentType "application/json" `
        -ErrorAction Stop
    Write-Host "      ✓ Utilisateur créé: $($response.username)" -ForegroundColor Green
}
catch {
    if ($_.Exception.Response -and $_.Exception.Response.StatusCode.value__ -eq 409) {
        Write-Host "      ! Utilisateur existe déjà (normal)" -ForegroundColor Yellow
    }
    else {
        Write-Host "      ✗ ERREUR lors de la création:" -ForegroundColor Red
        Write-Host "        $($_.Exception.Message)" -ForegroundColor Red
        if ($_.ErrorDetails.Message) {
            $errorObj = $_.ErrorDetails.Message | ConvertFrom-Json
            Write-Host "        Message: $($errorObj.message)" -ForegroundColor Red
        }
        return
    }
}


# Test 3: Login
Write-Host "`n[3/3] Test de connexion..." -ForegroundColor Yellow
$loginBody = @{
    email = "testlogin@example.com"
    password = "TestLogin123!"
} | ConvertTo-Json

try {
    $loginResponse = Invoke-RestMethod -Uri "http://localhost:8081/auth/login" `
        -Method POST `
        -Body $loginBody `
        -ContentType "application/json" `
        -ErrorAction Stop
    
    Write-Host "      ✓✓✓ LOGIN RÉUSSI ✓✓✓" -ForegroundColor Green
    Write-Host "`n      Détails de la session:" -ForegroundColor Cyan
    Write-Host "      • Utilisateur  : $($loginResponse.username)" -ForegroundColor White
    Write-Host "      • Email        : $($loginResponse.email)" -ForegroundColor White
    Write-Host "      • Rôle         : $($loginResponse.role)" -ForegroundColor White
    Write-Host "      • Access Token : $($loginResponse.accessToken.Substring(0, 40))..." -ForegroundColor White
    Write-Host "      • Refresh Token: $($loginResponse.refreshToken.Substring(0, 40))..." -ForegroundColor White
    
    # Test 4: Vérifier que le token fonctionne
    Write-Host "`n[BONUS] Test du token..." -ForegroundColor Yellow
    try {
        $headers = @{
            Authorization = "Bearer $($loginResponse.accessToken)"
        }
        $userInfo = Invoke-RestMethod -Uri "http://localhost:8081/api/users/me" `
            -Method GET `
            -Headers $headers `
            -ErrorAction Stop
        Write-Host "      ✓ Token valide - Session active!" -ForegroundColor Green
    }
    catch {
        Write-Host "      ! Token généré mais endpoint /api/users/me non disponible" -ForegroundColor Yellow
    }
}
catch {
    Write-Host "      ✗✗✗ ÉCHEC DU LOGIN ✗✗✗" -ForegroundColor Red
    Write-Host "`n      Code HTTP: $($_.Exception.Response.StatusCode.value__)" -ForegroundColor Red
    
    if ($_.ErrorDetails.Message) {
        try {
            $errorObj = $_.ErrorDetails.Message | ConvertFrom-Json
            Write-Host "      Message   : $($errorObj.message)" -ForegroundColor Red
            if ($errorObj.details) {
                Write-Host "      Détails   : $($errorObj.details)" -ForegroundColor Red
            }
        }
        catch {
            Write-Host "      Erreur    : $($_.ErrorDetails.Message)" -ForegroundColor Red
        }
    }
    else {
        Write-Host "      Erreur    : $($_.Exception.Message)" -ForegroundColor Red
    }
}


Write-Host "`n========================================`n" -ForegroundColor Cyan
