# ============================================================
#  Ji-AFK-Cinematic - Script de Compilacin Automatizada
#  Compila todos los grupos de versiones y los mueve a
#  la carpeta 'compilaciones'.
# ============================================================

$ErrorActionPreference = "Continue"
$RootDir = Get-Location
$CompiledDir = Join-Path $RootDir "compilaciones"

# Asegurar que el directorio de salida existe
if (-not (Test-Path $CompiledDir)) {
    New-Item -ItemType Directory -Path $CompiledDir -Force | Out-Null
}

$Groups = @(
    @{ Dir = "versions/A1-1.21-1.21.1";       Target = "ji-afk-cinematic-1.21-1.21.1-2.2.1.jar" }
    @{ Dir = "versions/A2-1.21.2-1.21.3";     Target = "ji-afk-cinematic-1.21.2-1.21.3-2.2.1.jar" }
    @{ Dir = "versions/A3-1.21.4";            Target = "ji-afk-cinematic-1.21.4-2.2.1.jar" }
    @{ Dir = "versions/A4-1.21.5";            Target = "ji-afk-cinematic-1.21.5-2.2.1.jar" }
    @{ Dir = "versions/A5-1.21.6-1.21.8";     Target = "ji-afk-cinematic-1.21.6-1.21.8-2.2.1.jar" }
    @{ Dir = "versions/A6-1.21.9-1.21.10";    Target = "ji-afk-cinematic-1.21.9-1.21.10-2.2.1.jar" }
    @{ Dir = "versions/A7-1.21.11";           Target = "ji-afk-cinematic-1.21.11-2.2.1.jar" }
    @{ Dir = "versions/B1-26.1-26.2";         Target = "ji-afk-cinematic-26.1-26.2-2.2.1.jar" }
)

$failures = @()

Write-Host "`n>>> Iniciando compilacin de todos los grupos...`n" -ForegroundColor Cyan

foreach ($g in $Groups) {
    $dirName = $g.Dir
    $targetName = $g.Target
    $groupPath = Join-Path $RootDir $dirName

    if (-not (Test-Path $groupPath)) {
        Write-Warning "No se encontr el directorio: $dirName. Saltando..."
        continue
    }

    Write-Host "--- Compilando $dirName ---" -ForegroundColor Yellow

    Push-Location $groupPath
    try {
        # Ejecutar Gradle (asumiendo gradlew existe)
        $gradleOutput = & .\gradlew.bat clean build --no-daemon 2>&1 | Out-String

        if ($gradleOutput -match 'BUILD SUCCESSFUL') {
            Write-Host "  [OK] Build exitoso" -ForegroundColor Green
        } else {
            Write-Host "  [FAIL] Build fallido" -ForegroundColor Red
            $failures += $dirName
            # Mostrar errores clave
            $gradleOutput -split "`n" | Where-Object { $_ -match 'error:|FAILED|>' } | Select-Object -First 10 | ForEach-Object { Write-Host "    $_" }
            Pop-Location
            Write-Host ""
            continue
        }

        # Localizar el JAR generado
        $libsDir = Join-Path $groupPath "build/libs"
        $jarFile = Get-ChildItem -Path $libsDir -Filter "*.jar" -ErrorAction SilentlyContinue | Where-Object { $_.Name -notmatch "sources|dev|shadow|api" } | Select-Object -First 1

        if ($jarFile) {
            $destPath = Join-Path $CompiledDir $targetName
            Write-Host "  Copiando $($jarFile.Name) -> $targetName" -ForegroundColor Green
            Copy-Item -Path $jarFile.FullName -Destination $destPath -Force
        } else {
            Write-Host "  [WARNING] No se encontr el JAR generado en $libsDir" -ForegroundColor Yellow
            $failures += $dirName
        }
    }
    catch {
        Write-Host "  [ERROR] Excepcin al compilar ${dirName}: $_" -ForegroundColor Red
        $failures += $dirName
    }
    finally {
        Pop-Location
    }
    Write-Host ""
}

Write-Host ">>> Proceso completado. JARs en '$CompiledDir'.`n" -ForegroundColor Cyan

if ($failures.Count -gt 0) {
    Write-Host "!!! Fallaron $($failures.Count) versiones:" -ForegroundColor Red
    $failures | ForEach-Object { Write-Host "  - $_" -ForegroundColor Red }
    exit 1
} else {
    Write-Host ">>> Todas las versiones compiladas correctamente." -ForegroundColor Green
}
