# =============================================================================
#  VERIFICA-WAR-DESPLEGADO.ps1
#
#  Comprueba sobre el artefacto REALMENTE DESPLEGADO que lleva las correcciones
#  22 y 10 del motor de nomina, y que trae los siete .jasper de RRHH.
#
#  Funciona con las dos formas de despliegue de WildFly:
#    - SaaBE.war como CARPETA  (despliegue explotado, el de Eclipse)
#    - SaaBE.war como ARCHIVO  (el .war empaquetado que se copia a mano)
#
#  Solo lee. No toca el despliegue ni reinicia nada.
#
#  USO:  .\verifica-war-desplegado.ps1 -Deploy "C:\wildfly\standalone\deployments\SaaBE.war"
# =============================================================================

param(
    [Parameter(Mandatory = $true)]
    [string]$Deploy,

    [string]$JavaHome = "C:\Program Files\Java\jdk-21"
)

$ErrorActionPreference = "Stop"
$CLASE = "com.saa.ejb.rhh.serviceImpl.ProcesoNominaServiceImpl"
$RUTA  = "WEB-INF/classes/com/saa/ejb/rhh/serviceImpl/ProcesoNominaServiceImpl.class"

# Concatenacion en vez de Join-Path: Join-Path revienta si la unidad no existe,
# y aqui una ruta de JDK equivocada tiene que degradar al respaldo, no abortar.
$javap = $JavaHome.TrimEnd('\') + "\bin\javap.exe"
$hayJavap = Test-Path $javap -ErrorAction SilentlyContinue
if (-not $hayJavap) {
    # RESPALDO SIN JDK. Un servidor de produccion puede tener solo JRE, o el
    # JDK en otra ruta. No hace falta javap: los nombres de metodo viven en el
    # POOL DE CONSTANTES del .class como texto UTF-8 plano, asi que basta con
    # buscarlos en los bytes. Es menos legible que javap y responde lo mismo.
    Write-Host ""
    Write-Host "AVISO: no hay javap en $javap" -ForegroundColor Yellow
    Write-Host "Se usa el respaldo por bytes, que no necesita JDK." -ForegroundColor Yellow
    Write-Host "Si prefieres javap, pasa la ruta del JDK con -JavaHome. Para buscarlo:" -ForegroundColor Yellow
    Write-Host '    Get-ChildItem "C:\Program Files\Java" -Directory' -ForegroundColor Yellow
}
if (-not (Test-Path $Deploy)) {
    Write-Host "ERROR: no existe $Deploy" -ForegroundColor Red
    exit 1
}

$item = Get-Item $Deploy
Write-Host ""
Write-Host "=== ARTEFACTO ===" -ForegroundColor Cyan
Write-Host ("  ruta            : " + $item.FullName)
Write-Host ("  ultima escritura: " + $item.LastWriteTime)
if ($item.PSIsContainer) {
    Write-Host "  forma           : CARPETA (despliegue explotado)"
} else {
    Write-Host ("  forma           : ARCHIVO (" + [math]::Round($item.Length / 1MB, 2) + " MB)")
}

# --- Se prepara un classpath legible por javap -------------------------------
$temporal = $null
if ($item.PSIsContainer) {
    $classpath = Join-Path $item.FullName "WEB-INF\classes"
    $jaspers = @(Get-ChildItem (Join-Path $item.FullName "WEB-INF\classes\rep\rhh") -Filter *.jasper -ErrorAction SilentlyContinue)
    $nombresJasper = $jaspers | ForEach-Object { $_.Name }
} else {
    Add-Type -AssemblyName System.IO.Compression.FileSystem
    $temporal = Join-Path $env:TEMP ("chk_saabe_" + [guid]::NewGuid().ToString("N").Substring(0, 8))
    New-Item -ItemType Directory -Path $temporal | Out-Null
    $zip = [System.IO.Compression.ZipFile]::OpenRead($item.FullName)
    try {
        # Un .war de Maven usa "/" como separador. Algunos empaquetadores de
        # Windows escriben "\". Se normaliza para que las dos formas encajen.
        $entrada = $zip.Entries | Where-Object { $_.FullName.Replace("\", "/") -eq $RUTA }
        if (-not $entrada) {
            Write-Host ""
            Write-Host "ERROR: el .war no contiene $RUTA" -ForegroundColor Red
            Write-Host "No es el WAR de SaaBE, o se empaqueto sin las clases." -ForegroundColor Red
            exit 1
        }
        $destino = Join-Path $temporal $RUTA.Replace("/", "\")
        New-Item -ItemType Directory -Path (Split-Path $destino) -Force | Out-Null
        [System.IO.Compression.ZipFileExtensions]::ExtractToFile($entrada, $destino, $true)
        $nombresJasper = @($zip.Entries |
            Where-Object { $_.FullName.Replace("\", "/") -like "WEB-INF/classes/rep/rhh/*.jasper" } |
            ForEach-Object { Split-Path $_.FullName -Leaf })
    } finally {
        $zip.Dispose()
    }
    $classpath = Join-Path $temporal "WEB-INF\classes"
}

# --- COMPROBACION 1: los metodos del motor -----------------------------------
Write-Host ""
Write-Host "=== COMPROBACION 1 - metodos de fondos de reserva ===" -ForegroundColor Cyan
if ($hayJavap) {
    $salida = & $javap -p -classpath $classpath $CLASE
    $texto = $salida -join "`n"
    foreach ($linea in $salida) {
        if ($linea -match "FondosReserva|superaUnAnio") { Write-Host ("  " + $linea.Trim()) }
    }
} else {
    $archivoClase = Join-Path $classpath "com\saa\ejb\rhh\serviceImpl\ProcesoNominaServiceImpl.class"
    $bytes = [System.IO.File]::ReadAllBytes($archivoClase)
    $texto = [System.Text.Encoding]::GetEncoding("ISO-8859-1").GetString($bytes)
    Write-Host ("  metodo leido por bytes sobre " + $bytes.Length + " bytes de clase")
    foreach ($n in @("fechaAniversarioFondosReserva", "baseFondosReservaProrrateada", "superaUnAnio")) {
        if ($texto.Contains($n)) { Write-Host ("    PRESENTE : " + $n) }
        else                     { Write-Host ("    ausente  : " + $n) }
    }
}

$tieneBase  = $texto -match "baseFondosReservaProrrateada"
$tieneAniv  = $texto -match "fechaAniversarioFondosReserva"
$tieneViejo = $texto -match "superaUnAnio"

Write-Host ""
if ($tieneBase -and $tieneAniv -and (-not $tieneViejo)) {
    Write-Host "  VEREDICTO: WAR NUEVO. Lleva las correcciones 22 y 10." -ForegroundColor Green
} elseif ($tieneViejo) {
    Write-Host "  VEREDICTO: WAR VIEJO. Aparece superaUnAnio." -ForegroundColor Red
    Write-Host "  NO se toca junio. El despliegue no surtio efecto." -ForegroundColor Red
} else {
    Write-Host "  VEREDICTO: RARO. Ni los metodos nuevos ni el viejo." -ForegroundColor Red
    Write-Host "  Reporta la salida completa de javap sin interpretarla." -ForegroundColor Red
}

# --- COMPROBACION 2: los siete .jasper de RRHH -------------------------------
Write-Host ""
Write-Host "=== COMPROBACION 2 - .jasper de RRHH ===" -ForegroundColor Cyan
$n = @($nombresJasper).Count
Write-Host ("  encontrados: " + $n + " de 7")
foreach ($j in ($nombresJasper | Sort-Object)) { Write-Host ("    " + $j) }
Write-Host ""
if ($n -eq 7) {
    Write-Host "  VEREDICTO: los siete estan. Los reportes de RRHH funcionaran." -ForegroundColor Green
} else {
    Write-Host "  VEREDICTO: FALTAN .jasper. En JasperReports 7.0.3 no hay" -ForegroundColor Red
    Write-Host "  compilacion en runtime: el reporte revienta al ejecutarlo." -ForegroundColor Red
}
Write-Host ""

if ($temporal -and (Test-Path $temporal)) { Remove-Item $temporal -Recurse -Force }
