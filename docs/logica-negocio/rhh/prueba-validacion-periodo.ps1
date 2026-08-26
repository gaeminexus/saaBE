# =============================================================================
#  PRUEBA-VALIDACION-PERIODO.ps1  —  corrección 16
#
#  Lanza los ocho casos inválidos contra el REST de LOCAL y muestra el mensaje
#  que devuelve cada uno. Después crea un período VÁLIDO, comprueba que se
#  guarda, y lo borra.
#
#  SOLO CONTRA LOCAL. No apuntar a producción: el caso 9 crea y borra un
#  registro real.
#
#  USO:  .\prueba-validacion-periodo.ps1
#        .\prueba-validacion-periodo.ps1 -Base "http://localhost:8080/SaaBE/rest"
# =============================================================================

param(
    [string]$Base = "http://localhost:8080/SaaBE/rest",
    [int]$Empresa = 1236
)

$ErrorActionPreference = "Continue"
$url = "$Base/prdn"

function Probar {
    param([string]$Titulo, [string]$Espera, [hashtable]$Cuerpo)

    Write-Host ""
    Write-Host "── $Titulo" -ForegroundColor Cyan
    Write-Host "   espera: $Espera" -ForegroundColor DarkGray

    $json = $Cuerpo | ConvertTo-Json -Depth 5 -Compress
    try {
        $r = Invoke-WebRequest -Uri $url -Method Post -Body $json `
             -ContentType "application/json" -UseBasicParsing
        Write-Host "   SE GUARDO (HTTP $($r.StatusCode)) — LA VALIDACION NO SALTO" -ForegroundColor Red
        Write-Host "   $($r.Content)" -ForegroundColor DarkGray
    } catch {
        $resp = $_.Exception.Response
        if ($resp) {
            $sr = New-Object System.IO.StreamReader($resp.GetResponseStream())
            $cuerpo = $sr.ReadToEnd(); $sr.Close()
            $codigo = [int]$resp.StatusCode
            if ($codigo -eq 500) {
                Write-Host "   RECHAZADO (HTTP $codigo)" -ForegroundColor Green
            } else {
                Write-Host "   HTTP $codigo — revisar" -ForegroundColor Yellow
            }
            Write-Host "   $cuerpo" -ForegroundColor Gray
        } else {
            Write-Host "   SIN RESPUESTA: $($_.Exception.Message)" -ForegroundColor Red
            Write-Host "   Compruebe que WildFly esta arriba y la ruta -Base es correcta." -ForegroundColor Yellow
        }
    }
}

# Un período de agosto de 2026 bien formado. Cada caso rompe UNA cosa.
function Base8 {
    return @{
        anio         = 2026
        mes          = 8
        fechaInicio  = "2026-08-01"
        fechaFin     = "2026-08-31"
        modo         = 1
        tipoPeriodo  = 1
        empresa      = @{ codigo = $Empresa }
    }
}

Write-Host ""
Write-Host "PRUEBA DE LA VALIDACION DEL PERIODO — correccion 16" -ForegroundColor White
Write-Host "Destino: $url" -ForegroundColor DarkGray
Write-Host "Los ocho primeros DEBEN ser rechazados. El noveno DEBE guardarse."

$c = Base8; $c.mes = 13
Probar "1 · Mes fuera de rango" "El mes declarado (13) no es valido" $c

$c = Base8; $c.Remove("fechaInicio"); $c.Remove("fechaFin")
Probar "2 · Sin fechas" "debe tener fecha de inicio (PRDNFCHI) y fecha de fin" $c

$c = Base8; $c.fechaInicio = "2026-01-01"
Probar "3 · Inicio fuera del mes declarado" "La fecha de inicio 2026-01-01 no corresponde" $c

$c = Base8; $c.fechaFin = "2026-09-05"
Probar "4 · Fin fuera del mes declarado" "La fecha de fin 2026-09-05 no corresponde" $c

$c = Base8; $c.fechaInicio = "2026-08-25"; $c.fechaFin = "2026-08-01"
Probar "5 · Inicio posterior al fin" "es posterior a la fecha de fin" $c

$c = Base8; $c.Remove("modo")
Probar "6 · Sin modo — EL QUE NOS MORDIO EL 25-08" "debe declarar el modo (PRDNMODO)" $c

$c = Base8; $c.Remove("tipoPeriodo")
Probar "7 · Sin tipo" "debe declarar el tipo (PRDNTPNM)" $c

$c = Base8; $c.mes = 6; $c.fechaInicio = "2026-06-01"; $c.fechaFin = "2026-06-30"
Probar "8 · Duplicado — junio ya existe en local" "Ya existe un periodo 2026-06 de tipo 1" $c

$c = Base8; $c.Remove("empresa")
Probar "8b · Sin empresa — el guard que anadio el backend" "debe declarar la empresa (PJRQCDGO)" $c

# ---------------------------------------------------------------------------
#  Caso valido: se crea y se borra.
#  Se usa DICIEMBRE para no chocar con ningun periodo real de local.
# ---------------------------------------------------------------------------
Write-Host ""
Write-Host "── 9 · CASO VALIDO — diciembre de prueba, se crea y se borra" -ForegroundColor Cyan
$c = Base8
$c.mes = 12; $c.fechaInicio = "2026-12-01"; $c.fechaFin = "2026-12-31"
$json = $c | ConvertTo-Json -Depth 5 -Compress
try {
    $r = Invoke-WebRequest -Uri $url -Method Post -Body $json `
         -ContentType "application/json" -UseBasicParsing
    Write-Host "   GUARDADO (HTTP $($r.StatusCode)) — la validacion deja pasar lo valido" -ForegroundColor Green
    $creado = $r.Content | ConvertFrom-Json
    $id = $creado.codigo
    Write-Host "   PRDN creado: $id" -ForegroundColor Gray
    if ($id) {
        try {
            Invoke-WebRequest -Uri "$url/$id" -Method Delete -UseBasicParsing | Out-Null
            Write-Host "   Borrado el PRDN $id. La base queda como estaba." -ForegroundColor Green
        } catch {
            Write-Host "   NO SE PUDO BORRAR el PRDN $id — BORRARLO A MANO:" -ForegroundColor Red
            Write-Host "   DELETE FROM RHH.PRDN WHERE PRDNCDGO = $id; COMMIT;" -ForegroundColor Yellow
        }
    }
} catch {
    $resp = $_.Exception.Response
    if ($resp) {
        $sr = New-Object System.IO.StreamReader($resp.GetResponseStream())
        Write-Host "   RECHAZADO, Y NO DEBIA SERLO:" -ForegroundColor Red
        Write-Host "   $($sr.ReadToEnd())" -ForegroundColor Gray
        $sr.Close()
    } else {
        Write-Host "   SIN RESPUESTA: $($_.Exception.Message)" -ForegroundColor Red
    }
}

Write-Host ""
Write-Host "Fin. Los ocho primeros en verde y el noveno guardado = correccion 16 correcta." -ForegroundColor White
Write-Host ""
