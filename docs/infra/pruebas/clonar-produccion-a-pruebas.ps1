# =============================================================================================
# Clona la base de PRODUCCION (contenedor oracle-23ai, puerto 1521) en la base de PRUEBAS
# (contenedor saa-pruebas, puerto 1531). Se puede volver a correr para refrescar pruebas.
#
# Uso, en el servidor, desde D:\saa-pruebas, en PowerShell como administrador:
#   powershell -ExecutionPolicy Bypass -File .\clonar-produccion-a-pruebas.ps1
#
# Que hace:
#   (con -Produccion <contenedor> si produccion no se llama saa-oracle-23ai)
#   1. Exporta de produccion los esquemas del SAA con expdp, sin detenerla (foto coherente con
#      FLASHBACK_TIME). En produccion solo se lee y se deja el archivo saa_pruebas.dmp en MY_DIR.
#   2. Copia el archivo a pruebas.
#   3. En PRUEBAS borra los esquemas del SAA (si existen) e importa.
#   4. Compara filas de PRST, APRT, ASNT y DTAS entre produccion y pruebas.
#   5. En PRUEBAS pone CBR.FCDR.AMBIENTE = 1 para que nada salga al SRI real.
#
# La clave de sys se pide al inicio, no se muestra y no queda escrita en ningun archivo.
# Sin tildes a proposito: Windows PowerShell 5.1 lee los .ps1 sin BOM como ANSI.
# =============================================================================================

param([string]$Produccion = 'saa-oracle-23ai')

$ErrorActionPreference = 'Continue'
$OutputEncoding = New-Object System.Text.UTF8Encoding $false

# Contenedor de produccion: saa-oracle-23ai (Docker Desktop lo muestra como "oracle-23ai", que es
# el nombre del servicio, no el del contenedor). Si en otro servidor se llamara distinto:
#   ... -File .\clonar-produccion-a-pruebas.ps1 -Produccion <nombre>   (ver: docker ps --format "{{.Names}}")
$PROD     = $Produccion
$PRUEBAS  = 'saa-pruebas'
$ESQUEMAS = 'SCP,CNT,TSR,CBR,PGS,CRD,RPR,RHH'
$DIR      = 'D:\saa-pruebas'
$DMP      = 'saa_pruebas.dmp'
$LOGS     = Join-Path $DIR ('logs-' + (Get-Date -Format 'yyyyMMdd-HHmm'))
New-Item -ItemType Directory -Force $LOGS | Out-Null

function Titulo($t) { Write-Host ''; Write-Host ('=' * 80) -ForegroundColor Cyan; Write-Host $t -ForegroundColor Cyan; Write-Host ('=' * 80) -ForegroundColor Cyan }
function Fallo($t)  { Write-Host ''; Write-Host "ERROR: $t" -ForegroundColor Red; Write-Host "Logs en $LOGS"; $env:SYSPWD = $null; exit 1 }

# Ejecuta un script de bash dentro de un contenedor, pasandolo por la entrada estandar
# (asi PowerShell no altera las comillas). Muestra la salida y la guarda en un log.
function EnContenedor($contenedor, $script, $log, [switch]$conSys) {
    $s = ($script -replace "`r", '') + "`n"
    $a = @('exec', '-i')
    if ($conSys) { $a += @('-e', 'SYSPWD') }
    $a += @($contenedor, 'bash', '-s')
    $res = $s | & docker @a 2>&1 | ForEach-Object { $l = "$_"; Write-Host $l; $l }
    $res | Set-Content -Path (Join-Path $LOGS $log)
    return $res
}

# ---------------------------------------------------------------------------------------------
Titulo '0. Verificaciones'
# ---------------------------------------------------------------------------------------------
# Lista de contenedores corriendo y comparacion exacta del nombre. (No se usa "docker inspect
# <nombre>": sin --type tambien busca imagenes, volumenes y redes con ese nombre.)
$corriendo = @(docker ps --format '{{.Names}}' 2>$null | ForEach-Object { "$_".Trim() })
foreach ($c in @($PROD, $PRUEBAS)) {
    if ($corriendo -notcontains $c) { Write-Host 'Contenedores que si existen:'; docker ps --format '  {{.Names}}  ({{.Ports}})'; Fallo "el contenedor $c no esta corriendo" }
}
$puertoProd = (docker port $PROD 1521 2>$null) -join ' '
if ($puertoProd -notmatch ':1521') { Fallo "$PROD no esta publicado en el 1521 ($puertoProd). No sigo: podria no ser produccion." }
$puerto = (docker port $PRUEBAS 1521 2>$null) -join ' '
if ($puerto -notmatch ':1531') { Fallo "$PRUEBAS no esta publicado en el 1531 ($puerto). No sigo: podria no ser la base de pruebas." }
Write-Host "Produccion: $PROD (1521)   Pruebas: $PRUEBAS (1531)   Esquemas: $ESQUEMAS"
Write-Host ''
Write-Host "ATENCION: en $PRUEBAS se BORRAN los esquemas $ESQUEMAS y se reemplazan por la copia de produccion." -ForegroundColor Yellow
Write-Host "Produccion solo se lee." -ForegroundColor Yellow
$ok = Read-Host 'Escribe PRUEBAS para continuar'
if ($ok -ne 'PRUEBAS') { Fallo 'cancelado por el usuario' }

$sec = Read-Host 'Clave de sys de PRODUCCION' -AsSecureString
$env:SYSPWD = [Runtime.InteropServices.Marshal]::PtrToStringAuto([Runtime.InteropServices.Marshal]::SecureStringToBSTR($sec))

# Prueba de la clave antes de exportar
$prueba = EnContenedor $PROD @'
sqlplus -s -l "sys/${SYSPWD}@localhost:1521/FREEPDB1 as sysdba" <<'EOF'
set heading off feedback off pages 0
select 'CLAVE_OK' from dual;
exit
EOF
'@ '00-prueba-clave.log' -conSys
if (-not ($prueba -match 'CLAVE_OK')) { Fallo 'la clave de sys de produccion no funciono' }

# ---------------------------------------------------------------------------------------------
Titulo '1. Exportando produccion (expdp, sin detenerla)'
# ---------------------------------------------------------------------------------------------
$exp = EnContenedor $PROD @"
PAR=/tmp/exp_saa_pruebas.par
cat > `$PAR <<EOF
userid="sys/`${SYSPWD}@localhost:1521/FREEPDB1 as sysdba"
schemas=$ESQUEMAS
directory=MY_DIR
dumpfile=$DMP
logfile=exp_saa_pruebas.log
flashback_time=systimestamp
reuse_dumpfiles=y
EOF
expdp parfile=`$PAR
rm -f `$PAR
"@ '01-export.log' -conSys
if (-not ($exp -match 'successfully completed|completed with')) { Fallo 'la exportacion no termino' }
if ($exp -match 'completed with') { Write-Host 'La exportacion termino CON errores: revisa las lineas ORA- de arriba.' -ForegroundColor Yellow }

# ---------------------------------------------------------------------------------------------
Titulo '2. Copiando el archivo a pruebas'
# ---------------------------------------------------------------------------------------------
docker cp "${PROD}:/opt/oracle/scripts/$DMP" "$DIR\$DMP"
if (-not (Test-Path "$DIR\$DMP")) { Fallo "no se pudo copiar $DMP desde $PROD" }
Write-Host ("Archivo: {0:N0} MB" -f ((Get-Item "$DIR\$DMP").Length / 1MB))
docker cp "$DIR\$DMP" "${PRUEBAS}:/opt/oracle/scripts/$DMP"
docker exec -u 0 $PRUEBAS chown 54321:54321 /opt/oracle/scripts "/opt/oracle/scripts/$DMP"

# ---------------------------------------------------------------------------------------------
Titulo '3. Importando en PRUEBAS'
# ---------------------------------------------------------------------------------------------
$lista = ($ESQUEMAS -split ',' | ForEach-Object { "'$_'" }) -join ','
$imp = EnContenedor $PRUEBAS @"
sqlplus -s -l "system/`${ORACLE_PWD}@localhost:1521/FREEPDB1" <<'EOF'
set serveroutput on feedback off
begin
  for u in (select username from dba_users where username in ($lista)) loop
    execute immediate 'drop user ' || u.username || ' cascade';
    dbms_output.put_line('borrado en pruebas: ' || u.username);
  end loop;
end;
/
create or replace directory MY_DIR as '/opt/oracle/scripts';
exit
EOF
impdp "system/`${ORACLE_PWD}@localhost:1521/FREEPDB1" directory=MY_DIR dumpfile=$DMP logfile=imp_saa_pruebas.log
"@ '03-import.log'
if (-not ($imp -match 'successfully completed|completed with')) { Fallo 'la importacion no termino' }
if ($imp -match 'completed with') { Write-Host 'La importacion termino CON errores: revisa las lineas ORA- de arriba.' -ForegroundColor Yellow }

# ---------------------------------------------------------------------------------------------
Titulo '4. Control: produccion contra pruebas'
# ---------------------------------------------------------------------------------------------
$consulta = @'
set heading off feedback off pages 0
select 'PRST '||count(*) from CRD.PRST;
select 'APRT '||count(*) from CRD.APRT;
select 'ASNT '||count(*) from CNT.ASNT;
select 'DTAS '||count(*) from CNT.DTAS;
select 'OBJETOS '||count(*) from dba_objects where owner in ('SCP','CNT','TSR','CBR','PGS','CRD','RPR','RHH');
select 'INVALIDOS '||count(*) from dba_objects where status='INVALID' and owner in ('SCP','CNT','TSR','CBR','PGS','CRD','RPR','RHH');
exit
'@
$cProd = EnContenedor $PROD ("sqlplus -s -l `"sys/`${SYSPWD}@localhost:1521/FREEPDB1 as sysdba`" <<'EOF'`n" + $consulta + "`nEOF") '04-control-prod.log' -conSys
$cPrue = EnContenedor $PRUEBAS ("sqlplus -s -l `"system/`${ORACLE_PWD}@localhost:1521/FREEPDB1`" <<'EOF'`n" + $consulta + "`nEOF") '04-control-pruebas.log'
$env:SYSPWD = $null

function AMapa($lineas) { $m = @{}; foreach ($l in $lineas) { if ("$l" -match '^\s*([A-Z]+)\s+(\d+)\s*$') { $m[$matches[1]] = [long]$matches[2] } }; $m }
$mp = AMapa $cProd; $mq = AMapa $cPrue
Write-Host ''
'{0,-10} {1,12} {2,12}  {3}' -f 'TABLA', 'PRODUCCION', 'PRUEBAS', '' | Write-Host
foreach ($k in @('PRST', 'APRT', 'ASNT', 'DTAS', 'OBJETOS', 'INVALIDOS')) {
    $estado = if ($mp[$k] -eq $mq[$k]) { 'OK' } else { 'DIFERENTE' }
    $color  = if ($estado -eq 'OK') { 'Green' } else { 'Yellow' }
    Write-Host ('{0,-10} {1,12} {2,12}  {3}' -f $k, $mp[$k], $mq[$k], $estado) -ForegroundColor $color
}
Write-Host '(ASNT/DTAS pueden diferir en pocas filas si alguien trabajo en produccion despues de la foto.)'

# ---------------------------------------------------------------------------------------------
Titulo '5. Pruebas: SRI en ambiente de pruebas'
# ---------------------------------------------------------------------------------------------
EnContenedor $PRUEBAS @'
sqlplus -s -l "system/${ORACLE_PWD}@localhost:1521/FREEPDB1" <<'EOF'
set heading off feedback off pages 0
select 'antes,   facturadores en SRI de PRODUCCION: '||count(*) from CBR.FCDR where AMBIENTE = 2;
update CBR.FCDR set AMBIENTE = 1 where nvl(AMBIENTE, 0) <> 1;
commit;
select 'despues, facturadores en SRI de PRODUCCION: '||count(*) from CBR.FCDR where AMBIENTE = 2;
exit
EOF
'@ '05-sri.log' | Out-Null

Titulo 'TERMINADO'
Write-Host "Logs en $LOGS"
Write-Host "Pruebas queda en el puerto 1531 (servicio FREEPDB1), con los mismos usuarios y claves que produccion."
