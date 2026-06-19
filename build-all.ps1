# ============================================================
#  Ji-AFK-Cinematic - Script de Compilación Automatizada
#  Compila todos los grupos de versiones y los mueve a
#  la carpeta 'compilaciones'.
# ============================================================

$ErrorActionPreference = "Stop"
$RootDir = Get-Location
$CompiledDir = Join-Path $RootDir "compilaciones"

# Asegurar que el directorio de salida existe
if (-not (Test-Path $CompiledDir)) {
    New-Item -ItemType Directory -Path $CompiledDir -Force | Out-Null
}

$Groups = @(
    @{ Dir = "versions/A1-1.21-1.21.1";       Target = "ji-afk-cinematic-1.21-1.21.1-2.2.0.jar" }
    @{ Dir = "versions/A2-1.21.2-1.21.3";     Target = "ji-afk-cinematic-1.21.2-1.21.3-2.2.0.jar" }
    @{ Dir = "versions/A3-1.21.4";            Target = "ji-afk-cinematic-1.21.4-2.2.0.jar" }
    @{ Dir = "versions/A4-1.21.5";            Target = "ji-afk-cinematic-1.21.5-2.2.0.jar" }
    @{ Dir = "versions/A5-1.21.6-1.21.8";     Target = "ji-afk-cinematic-1.21.6-1.21.8-2.2.0.jar" }
    @{ Dir = "versions/A6-1.21.9-1.21.10";    Target = "ji-afk-cinematic-1.21.9-1.21.10-2.2.0.jar" }
    @{ Dir = "versions/A7-1.21.11";           Target = "ji-afk-cinematic-1.21.11-2.2.0.jar" }
    @{ Dir = "versions/B1-26.1-26.2";         Target = "ji-afk-cinematic-26.1-26.2-2.2.0.jar" }
)

Write-Host "`n>>> Iniciando compilación de todos los grupos...`n" -ForegroundColor Cyan

foreach ($g in $Groups) {
    $dirName = $g.Dir
    $targetName = $g.Target
    $groupPath = Join-Path $RootDir $dirName
    
    if (-not (Test-Path $groupPath)) {
        Write-Warning "No se encontró el directorio: $dirName. Saltando..."
        continue
    }

    Write-Host "--- Compilando $dirName ---" -ForegroundColor Yellow
    
    Push-Location $groupPath
    try {
        # Ejecutar Gradle (asumiendo gradlew existe)
        if (Test-Path "./gradlew.bat") {
            ./gradlew.bat clean build
        } else {
            gradle clean build
        }

        # Localizar el JAR generado
        $libsDir = Join-Path $groupPath "build/libs"
        # Buscamos el jar que no sea -sources o -dev o -shadow
        $jarFile = Get-ChildItem -Path $libsDir -Filter "*.jar" | Where-Object { $_.Name -notmatch "sources|dev|shadow|api" } | Select-Object -First 1
        
        if ($jarFile) {
            $destPath = Join-Path $CompiledDir $targetName
            Write-Host "  Copiando $($jarFile.Name) -> $targetName" -ForegroundColor Green
            Copy-Item -Path $jarFile.FullName -Destination $destPath -Force
        } else {
            Write-Error "  No se encontró el JAR generado en $libsDir"
        }
    }
    catch {
        Write-Error "  Error al compilar $dirName : $_"
    }
    finally {
        Pop-Location
    }
    Write-Host ""
}

Write-Host ">>> Proceso completado. Los JARs están en '$CompiledDir'.`n" -ForegroundColor Cyan
