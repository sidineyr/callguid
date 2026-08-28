[CmdletBinding()]
param(
    [switch]$Install
)

$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$gradle = Join-Path $projectRoot 'gradlew.bat'
$apk = Join-Path $projectRoot 'app\build\outputs\apk\debug\app-debug.apk'

if (-not $env:JAVA_HOME) {
    $studioJava = Join-Path $env:ProgramFiles 'Android\Android Studio\jbr'
    if (Test-Path $studioJava) {
        $env:JAVA_HOME = $studioJava
    }
}

if (-not $env:ANDROID_HOME -and $env:LOCALAPPDATA) {
    $defaultSdk = Join-Path $env:LOCALAPPDATA 'Android\Sdk'
    if (Test-Path $defaultSdk) {
        $env:ANDROID_HOME = $defaultSdk
    }
}

if (-not $env:JAVA_HOME -or -not (Test-Path $env:JAVA_HOME)) {
    throw 'Java não encontrado. Instale o Android Studio ou defina JAVA_HOME.'
}

if (-not $env:ANDROID_HOME -or -not (Test-Path $env:ANDROID_HOME)) {
    throw 'Android SDK não encontrado. Abra o Android Studio, instale o SDK 33 e tente novamente.'
}

Push-Location $projectRoot
try {
    & $gradle testDebugUnitTest assembleDebug --no-daemon --stacktrace
    if ($LASTEXITCODE -ne 0) {
        throw "O Gradle terminou com o código $LASTEXITCODE."
    }
} finally {
    Pop-Location
}

if (-not (Test-Path $apk)) {
    throw "O build terminou sem criar o APK esperado: $apk"
}

Write-Host "APK de teste criado: $apk" -ForegroundColor Green

if ($Install) {
    $adb = Join-Path $env:ANDROID_HOME 'platform-tools\adb.exe'
    if (-not (Test-Path $adb)) {
        throw 'ADB não encontrado. Instale Android SDK Platform-Tools pelo Android Studio.'
    }
    & $adb install -r $apk
    if ($LASTEXITCODE -ne 0) {
        throw 'Não foi possível instalar. Ative a depuração USB e autorize este computador no celular.'
    }
    Write-Host 'CallGuide instalado no aparelho.' -ForegroundColor Green
}
