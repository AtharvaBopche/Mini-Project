$ErrorActionPreference = 'Stop'

foreach ($name in 'DB_URL', 'DB_USER', 'DB_PASSWORD') {
    $value = [Environment]::GetEnvironmentVariable($name, 'User')
    if ($null -eq $value) {
        Remove-Item "Env:$name" -ErrorAction SilentlyContinue
    } else {
        Set-Item "Env:$name" $value
    }
}

$maven = Get-Command 'mvn' -ErrorAction SilentlyContinue
if ($maven) {
    $mavenCommand = 'mvn'
} else {
    $maven = Join-Path $env:LOCALAPPDATA 'Programs\Apache\Maven\apache-maven-3.10.0\bin\mvn.cmd'
    if (-not (Test-Path -LiteralPath $maven)) {
        throw 'Maven was not found. Install Maven and add it to PATH, then run this launcher again.'
    }
    $mavenCommand = "'$maven'"
}

$backend = Join-Path $PSScriptRoot 'backend'
$frontend = Join-Path $PSScriptRoot 'frontend'
$backendCommand = "`$env:SPRING_PROFILES_ACTIVE='local'; Set-Location -LiteralPath '$backend'; $mavenCommand spring-boot:run"

Start-Process -FilePath 'powershell.exe' -WorkingDirectory $backend -ArgumentList @('-NoExit', '-Command', $backendCommand)
$frontendCommand = "Set-Location -LiteralPath '$frontend'; if (-not (Test-Path -LiteralPath 'node_modules')) { npm install; if (`$LASTEXITCODE -ne 0) { throw 'npm install failed' } }; npm run dev -- --host 127.0.0.1"
Start-Process -FilePath 'powershell.exe' -WorkingDirectory $frontend -ArgumentList @('-NoExit', '-Command', $frontendCommand)

$apiReady = $false
for ($attempt = 0; $attempt -lt 60; $attempt++) {
    try {
        $response = Invoke-WebRequest -Uri 'http://127.0.0.1:8081/api/health' -TimeoutSec 2 -UseBasicParsing
        if ($response.StatusCode -eq 200) { $apiReady = $true; break }
    } catch {
        Start-Sleep -Seconds 1
    }
}

if (-not $apiReady) {
    Write-Warning 'The API did not become ready within 60 seconds. Check the backend window for errors.'
}
Start-Process 'http://127.0.0.1:5173/'
