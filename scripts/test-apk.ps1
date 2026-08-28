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
    throw 'Java was not found. Install Android Studio or define JAVA_HOME.'
}

if (-not $env:ANDROID_HOME -or -not (Test-Path $env:ANDROID_HOME)) {
    throw 'The Android SDK was not found. Open Android Studio, install SDK 33, and try again.'
}

Push-Location $projectRoot
try {
    & $gradle testDebugUnitTest assembleDebug --no-daemon --stacktrace
    if ($LASTEXITCODE -ne 0) {
        throw "Gradle exited with code $LASTEXITCODE."
    }
} finally {
    Pop-Location
}

if (-not (Test-Path $apk)) {
    throw "The build finished without creating the expected APK: $apk"
}

Write-Host "Debug APK created: $apk" -ForegroundColor Green

if ($Install) {
    $adb = Join-Path $env:ANDROID_HOME 'platform-tools\adb.exe'
    if (-not (Test-Path $adb)) {
        throw 'ADB was not found. Install Android SDK Platform-Tools through Android Studio.'
    }
    & $adb install -r $apk
    if ($LASTEXITCODE -ne 0) {
        throw 'Installation failed. Enable USB debugging and authorize this computer on the Android device.'
    }
    Write-Host 'CallGuide was installed on the device.' -ForegroundColor Green
}
