# =============================================================================
# Levantamiento de un servidor del SAA para la documentacion de auditoria
# -----------------------------------------------------------------------------
# SOLO LECTURA. No instala, no cambia configuracion, no escribe en la base.
# Lo unico que crea es el archivo de salida.
#
# Como correrlo (PowerShell COMO ADMINISTRADOR, en cada servidor, .3 y .4):
#   powershell -ExecutionPolicy Bypass -File D:\saa-auditoria\levantamiento-servidor.ps1
#
# Salida: D:\saa-auditoria\levantamiento-<SERVIDOR>-<fecha>.txt
#   (si D: no existe, en la carpeta del usuario)
#
# Que NO sale de aqui y hay que completarlo a mano: contrato y garantia
# (portal de Dell con el Service Tag que imprime este script), nivel de RAID y
# fuentes de poder si la controladora es PERC (iDRAC), ubicacion fisica,
# equipos de red, UPS, proveedor de internet y nombres/areas de las personas.
#
# Nunca imprime contrasenas: las lineas de configuracion y de procesos pasan
# por un filtro que tapa cualquier valor de password/clave/secret/key.
# Los usuarios de la app movil (participes) salen solo como conteo por estado.
# =============================================================================

param(
    [string]$Carpeta = $(if (Test-Path 'D:\') { 'D:\saa-auditoria' } else { Join-Path $env:USERPROFILE 'saa-auditoria' }),
    [string]$Pdb = 'FREEPDB1',
    [int[]]$PuertosSaa = @(443, 1521, 1531, 3389, 8080, 8180, 8443, 8543, 9990, 10090)
)

$ErrorActionPreference = 'Continue'
# UTF-8 sin BOM: un BOM al inicio del SQL que va a sqlplus rompe el primer comando
$OutputEncoding = New-Object System.Text.UTF8Encoding $false
New-Item -ItemType Directory -Force -Path $Carpeta | Out-Null
$salida = Join-Path $Carpeta ("levantamiento-{0}-{1}.txt" -f $env:COMPUTERNAME, (Get-Date -Format 'yyyyMMdd-HHmm'))

function Tapar([string]$texto) {
    if ($null -eq $texto) { return $texto }
    $t = $texto -replace '(?i)((password|passwd|pwd|secret|clave|key|token)[^=:\s"<>]*\s*[=:]\s*)("[^"]*"|\S+)', '$1***'
    $t = $t -replace '(?i)(<password>)[^<]*(</password>)', '$1***$2'
    return $t
}

function Escribir([string]$texto) {
    Add-Content -Path $salida -Value (Tapar $texto) -Encoding UTF8
}

function Seccion([string]$titulo, [scriptblock]$bloque) {
    Escribir ''
    Escribir ('=' * 100)
    Escribir "== $titulo"
    Escribir ('=' * 100)
    try {
        $r = & $bloque 2>&1 | Out-String -Width 300
        if ([string]::IsNullOrWhiteSpace($r)) { $r = '(sin datos)' }
        Escribir $r
    } catch {
        Escribir "ERROR en esta seccion: $($_.Exception.Message)"
    }
    Write-Host "  listo: $titulo"
}

Escribir "LEVANTAMIENTO DE SERVIDOR - SAA"
Escribir "Servidor: $env:COMPUTERNAME   Fecha: $(Get-Date -Format 'yyyy-MM-dd HH:mm')   Usuario que corre: $env:USERDOMAIN\$env:USERNAME"
Write-Host "Escribiendo en $salida"

# ----------------------------------------------------------------------------- 1. Hardware
Seccion '1.1 Equipo, fabricante, modelo y numero de serie (Service Tag)' {
    $cs = Get-CimInstance Win32_ComputerSystem
    $bios = Get-CimInstance Win32_BIOS
    [pscustomobject]@{
        Nombre        = $cs.Name
        Fabricante    = $cs.Manufacturer
        Modelo        = $cs.Model
        ServiceTag    = $bios.SerialNumber
        BIOS          = "$($bios.SMBIOSBIOSVersion) ($($bios.ReleaseDate))"
        Dominio       = $cs.Domain
        EnDominio     = $cs.PartOfDomain
        MemoriaGB     = [math]::Round($cs.TotalPhysicalMemory / 1GB, 1)
    } | Format-List
}

Seccion '1.2 Procesador' {
    Get-CimInstance Win32_Processor | Select-Object Name, NumberOfCores, NumberOfLogicalProcessors, MaxClockSpeed, SocketDesignation | Format-List
}

Seccion '1.3 Modulos de memoria' {
    Get-CimInstance Win32_PhysicalMemory | Select-Object BankLabel, DeviceLocator, @{n='GB';e={[math]::Round($_.Capacity/1GB,1)}}, Speed, Manufacturer, PartNumber | Format-Table -AutoSize
    $slots = (Get-CimInstance Win32_PhysicalMemoryArray | Measure-Object -Property MemoryDevices -Sum).Sum
    "Ranuras de memoria totales: $slots"
}

Seccion '1.4 Discos fisicos, controladora y volumenes' {
    'Controladoras de almacenamiento:'
    Get-CimInstance Win32_SCSIController | Select-Object Name, DriverName | Format-Table -AutoSize
    'Discos fisicos que ve Windows (con una PERC en RAID, aqui aparece el disco virtual):'
    Get-PhysicalDisk | Select-Object FriendlyName, MediaType, BusType, @{n='GB';e={[math]::Round($_.Size/1GB)}}, HealthStatus, OperationalStatus | Format-Table -AutoSize
    'Discos virtuales de Storage Spaces (si los hay):'
    Get-VirtualDisk -ErrorAction SilentlyContinue | Select-Object FriendlyName, ResiliencySettingName, @{n='GB';e={[math]::Round($_.Size/1GB)}}, HealthStatus | Format-Table -AutoSize
    'Volumenes:'
    Get-Volume | Where-Object DriveLetter | Select-Object DriveLetter, FileSystemLabel, FileSystem, @{n='GB';e={[math]::Round($_.Size/1GB)}}, @{n='LibreGB';e={[math]::Round($_.SizeRemaining/1GB)}}, HealthStatus | Format-Table -AutoSize
}

Seccion '1.5 RAID segun herramientas de Dell (solo si estan instaladas)' {
    $omreport = Get-Command omreport -ErrorAction SilentlyContinue
    $perccli  = Get-Command perccli64, perccli -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($omreport) { & omreport storage vdisk; & omreport chassis pwrsupplies }
    elseif ($perccli) { & $perccli.Source /call/vall show }
    else { 'No hay OpenManage ni perccli instalados: el nivel de RAID y las fuentes de poder se ven en el iDRAC del servidor.' }
}

# ----------------------------------------------------------------------------- 2. Sistema operativo y red
Seccion '2.1 Sistema operativo' {
    $os = Get-CimInstance Win32_OperatingSystem
    [pscustomobject]@{
        Sistema        = $os.Caption
        Version        = $os.Version
        Compilacion    = $os.BuildNumber
        Instalado      = $os.InstallDate
        UltimoArranque = $os.LastBootUpTime
        Idioma         = $os.MUILanguages -join ','
    } | Format-List
}

Seccion '2.2 Licencia de Windows' {
    Get-CimInstance SoftwareLicensingProduct -Filter "ApplicationID='55c92734-d682-4d71-983e-d6ec3f16059f' AND PartialProductKey IS NOT NULL" |
        Select-Object Name, Description, @{n='Estado';e={switch ($_.LicenseStatus) {1 {'Licenciado'} 0 {'Sin licencia'} default {"Codigo $($_.LicenseStatus)"}}}}, PartialProductKey | Format-List
}

Seccion '2.3 Ultimas actualizaciones instaladas' {
    Get-HotFix | Sort-Object InstalledOn -Descending | Select-Object -First 10 HotFixID, Description, InstalledOn | Format-Table -AutoSize
}

Seccion '2.4 Red: adaptadores, IP y DNS' {
    Get-NetAdapter | Select-Object Name, InterfaceDescription, Status, LinkSpeed, MacAddress | Format-Table -AutoSize
    Get-NetIPAddress -AddressFamily IPv4 | Where-Object IPAddress -ne '127.0.0.1' | Select-Object InterfaceAlias, IPAddress, PrefixLength, PrefixOrigin | Format-Table -AutoSize
    Get-NetIPConfiguration | Select-Object InterfaceAlias, @{n='Gateway';e={$_.IPv4DefaultGateway.NextHop}}, @{n='DNS';e={$_.DNSServer.ServerAddresses -join ','}} | Format-Table -AutoSize
}

Seccion '2.5 Puertos escuchando y proceso duenio' {
    Get-NetTCPConnection -State Listen | Sort-Object LocalPort -Unique |
        Select-Object LocalAddress, LocalPort, @{n='Proceso';e={(Get-Process -Id $_.OwningProcess -ErrorAction SilentlyContinue).ProcessName}} | Format-Table -AutoSize
}

# ----------------------------------------------------------------------------- 3. Software base
Seccion '3.1 Programas instalados' {
    $rutas = 'HKLM:\SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall\*', 'HKLM:\SOFTWARE\WOW6432Node\Microsoft\Windows\CurrentVersion\Uninstall\*'
    Get-ItemProperty $rutas -ErrorAction SilentlyContinue | Where-Object DisplayName |
        Sort-Object DisplayName -Unique | Select-Object DisplayName, DisplayVersion, Publisher, InstallDate | Format-Table -AutoSize
}

Seccion '3.2 Docker: version, contenedores, imagenes y volumenes' {
    if (-not (Get-Command docker -ErrorAction SilentlyContinue)) { 'Docker no esta en el PATH de este usuario.'; return }
    docker version
    ''
    docker ps -a --format 'table {{.Names}}\t{{.Image}}\t{{.Status}}\t{{.Ports}}'
    ''
    docker images --digests --format 'table {{.Repository}}\t{{.Tag}}\t{{.Digest}}\t{{.CreatedSince}}\t{{.Size}}'
    ''
    docker volume ls
    ''
    'Arranque de Docker: si corre como servicio de Windows o dentro de la sesion de un usuario'
    Get-Service *docker* -ErrorAction SilentlyContinue | Select-Object Name, Status, StartType | Format-Table -AutoSize
}

Seccion '3.3 Java' {
    $j = Get-Command java -ErrorAction SilentlyContinue
    if ($j) { "java en el PATH: $($j.Source)"; cmd /c "java -version 2>&1" } else { 'No hay java en el PATH.' }
    ''
    'Procesos Java en ejecucion (ruta del ejecutable y parametros, con secretos tapados):'
    Get-CimInstance Win32_Process -Filter "Name='java.exe'" | Select-Object ProcessId, ExecutablePath, CommandLine | Format-List
}

Seccion '3.4 WildFly: instalaciones, version, memoria y datasource' {
    $raices = @('C:\', 'D:\', 'E:\') | Where-Object { Test-Path $_ }
    $wf = foreach ($r in $raices) { Get-ChildItem $r -Directory -ErrorAction SilentlyContinue | Where-Object { $_.Name -match 'wildfly|jboss' } }
    if (-not $wf) { 'No se encontraron carpetas de WildFly en la raiz de los discos.'; return }
    foreach ($d in $wf) {
        "---- $($d.FullName)"
        $log = Join-Path $d.FullName 'standalone\log\server.log'
        if (Test-Path $log) {
            'Version (ultimo arranque registrado):'
            Select-String -Path $log -Pattern 'WFLYSRV0049' | Select-Object -Last 1 | ForEach-Object { $_.Line }
        }
        $conf = Join-Path $d.FullName 'bin\standalone.conf.bat'
        if (Test-Path $conf) {
            'Memoria y propiedades configuradas (standalone.conf.bat):'
            Select-String -Path $conf -Pattern 'Xmx|Xms|MaxMetaspace|port-offset|saa\.' | ForEach-Object { $_.Line.Trim() }
        }
        $xml = Join-Path $d.FullName 'standalone\configuration\standalone.xml'
        if (Test-Path $xml) {
            'Datasources (solo URL y usuario; la contrasena no se lee):'
            Select-String -Path $xml -Pattern '<datasource |<connection-url>|<user-name>|<security ' | ForEach-Object { $_.Line.Trim() }
            'Sesiones de correo (servidor y puerto):'
            Select-String -Path $xml -Pattern '<mail-session|outbound-socket-binding name="mail|remote-destination host' | ForEach-Object { $_.Line.Trim() }
        }
        $dep = Join-Path $d.FullName 'standalone\deployments'
        if (Test-Path $dep) {
            'Desplegados:'
            Get-ChildItem $dep -File | Where-Object { $_.Extension -in '.war', '.ear', '.deployed', '.failed' } | Select-Object Name, Length, LastWriteTime | Format-Table -AutoSize | Out-String -Width 300
        }
    }
}

Seccion '3.5 Servicios automaticos que no son de Windows' {
    Get-CimInstance Win32_Service | Where-Object { $_.StartMode -eq 'Auto' -and $_.PathName -notmatch '\\Windows\\' } |
        Select-Object Name, DisplayName, State, StartName | Format-Table -AutoSize
}

Seccion '3.6 Tareas programadas que no son de Microsoft (respaldos, clonados, etc.)' {
    Get-ScheduledTask | Where-Object { $_.TaskPath -notlike '\Microsoft*' } | ForEach-Object {
        $i = $_ | Get-ScheduledTaskInfo -ErrorAction SilentlyContinue
        [pscustomobject]@{
            Tarea      = "$($_.TaskPath)$($_.TaskName)"
            Estado     = $_.State
            Ejecuta    = ($_.Actions | ForEach-Object { "$($_.Execute) $($_.Arguments)" }) -join ' | '
            UltimaVez  = $i.LastRunTime
            Proxima    = $i.NextRunTime
        }
    } | Format-List
}

Seccion '3.7 Respaldos de Windows Server Backup (si esta instalado)' {
    if (Get-Command wbadmin -ErrorAction SilentlyContinue) { wbadmin get versions } else { 'Windows Server Backup no esta instalado.' }
}

# ----------------------------------------------------------------------------- 4. Usuarios y seguridad del servidor
Seccion '4.1 Usuarios locales' {
    Get-LocalUser | Select-Object Name, FullName, Enabled, LastLogon, PasswordLastSet, PasswordExpires, Description | Format-Table -AutoSize
}

Seccion '4.2 Administradores y usuarios de escritorio remoto' {
    'Administradores:'
    Get-LocalGroupMember -SID 'S-1-5-32-544' | Select-Object Name, ObjectClass, PrincipalSource | Format-Table -AutoSize
    'Usuarios de escritorio remoto:'
    Get-LocalGroupMember -SID 'S-1-5-32-555' -ErrorAction SilentlyContinue | Select-Object Name, ObjectClass, PrincipalSource | Format-Table -AutoSize
}

Seccion '4.3 Politica de contrasenas local' {
    net accounts
}

Seccion '4.4 Escritorio remoto' {
    $ts = Get-ItemProperty 'HKLM:\System\CurrentControlSet\Control\Terminal Server' -ErrorAction SilentlyContinue
    $rdp = Get-ItemProperty 'HKLM:\System\CurrentControlSet\Control\Terminal Server\WinStations\RDP-Tcp' -ErrorAction SilentlyContinue
    [pscustomobject]@{
        EscritorioRemotoHabilitado = ($ts.fDenyTSConnections -eq 0)
        ExigeNLA                   = ($rdp.UserAuthentication -eq 1)
        Puerto                     = $rdp.PortNumber
    } | Format-List
    'Otras herramientas de acceso remoto instaladas (AnyDesk, TeamViewer, etc.) aparecen en 3.1.'
}

Seccion '4.5 Antivirus (Microsoft Defender)' {
    $mp = Get-MpComputerStatus -ErrorAction SilentlyContinue
    if ($mp) {
        $mp | Select-Object AMProductVersion, AntivirusEnabled, RealTimeProtectionEnabled, AntivirusSignatureVersion, AntivirusSignatureLastUpdated, QuickScanEndTime, FullScanEndTime | Format-List
    } else { 'Defender no responde: puede haber otro antivirus (revisar 3.1 y 3.5).' }
}

Seccion '4.6 Firewall de Windows: perfiles y reglas de entrada de los puertos del SAA' {
    Get-NetFirewallProfile | Select-Object Name, Enabled, DefaultInboundAction, DefaultOutboundAction | Format-Table -AutoSize
    Get-NetFirewallRule -Direction Inbound -Enabled True | ForEach-Object {
        $pf = $_ | Get-NetFirewallPortFilter
        $puertos = @($pf.LocalPort) | Where-Object { $_ -match '^\d+$' } | ForEach-Object { [int]$_ }
        if ($puertos | Where-Object { $PuertosSaa -contains $_ }) {
            $af = $_ | Get-NetFirewallAddressFilter
            [pscustomobject]@{
                Regla    = $_.DisplayName
                Accion   = $_.Action
                Puertos  = ($pf.LocalPort -join ',')
                Origen   = ($af.RemoteAddress -join ',')
                Perfil   = $_.Profile
            }
        }
    } | Format-Table -AutoSize
}

Seccion '4.7 Cifrado de disco (BitLocker)' {
    if (Get-Command Get-BitLockerVolume -ErrorAction SilentlyContinue) {
        Get-BitLockerVolume | Select-Object MountPoint, VolumeStatus, ProtectionStatus, EncryptionMethod | Format-Table -AutoSize
    } else { 'BitLocker no esta instalado.' }
}

# ----------------------------------------------------------------------------- 5. Bases de datos Oracle (dentro de cada contenedor)
$sql = @"
SET MARKUP CSV ON QUOTE ON
SET FEEDBACK ON
SET PAGESIZE 50000
WHENEVER SQLERROR CONTINUE
PROMPT ### Version de la base
SELECT banner_full FROM v`$version;
ALTER SESSION SET CONTAINER = $Pdb;
PROMPT ### A.4 Usuarios de base de datos
SELECT username, account_status, lock_date, expiry_date, profile, default_tablespace, created, last_login
  FROM dba_users WHERE oracle_maintained = 'N' ORDER BY username;
PROMPT ### A.4 Roles por usuario
SELECT grantee, granted_role, admin_option FROM dba_role_privs
 WHERE grantee IN (SELECT username FROM dba_users WHERE oracle_maintained = 'N') ORDER BY grantee, granted_role;
PROMPT ### A.4 Privilegios de sistema por usuario
SELECT grantee, privilege, admin_option FROM dba_sys_privs
 WHERE grantee IN (SELECT username FROM dba_users WHERE oracle_maintained = 'N') ORDER BY grantee, privilege;
PROMPT ### A.4 Permisos de SCP sobre otros esquemas
SELECT grantee, owner, privilege, COUNT(*) AS objetos FROM dba_tab_privs WHERE grantee = 'SCP'
 GROUP BY grantee, owner, privilege ORDER BY owner, privilege;
PROMPT ### A.4 Politica de contrasenas de los perfiles en uso
SELECT profile, resource_name, limit FROM dba_profiles
 WHERE resource_type = 'PASSWORD'
   AND profile IN (SELECT DISTINCT profile FROM dba_users WHERE oracle_maintained = 'N')
 ORDER BY profile, resource_name;
PROMPT ### Tamano de datos de usuario (limite de Oracle Free: 12 GB)
SELECT owner, ROUND(SUM(bytes)/1024/1024) AS mb FROM dba_segments
 WHERE owner IN (SELECT username FROM dba_users WHERE oracle_maintained = 'N')
 GROUP BY owner ORDER BY mb DESC;
PROMPT ### A.1 Usuarios de la aplicacion SAA
SELECT u.PJRQCDGO AS codigo_usuario, u.PJRQNMBR AS usuario_login, p.PJRQNMBR AS nodo_padre, j.PGSPNMBR AS jerarquia,
       CASE (SELECT MAX(d.PDCNVLRR) FROM SCP.PDCN d WHERE d.PCXNCDGO = 19 AND d.PJRQCDGO = u.PJRQCDGO)
            WHEN '1' THEN 'ACTIVO' WHEN '2' THEN 'INACTIVO' ELSE 'SIN DATO' END AS estado
  FROM SCP.PJRQ u JOIN SCP.PGSP j ON j.PGSPCDGO = u.PGSPCDGO
  LEFT JOIN SCP.PJRQ p ON p.PJRQCDGO = u.PJRQCDPD
 WHERE u.PGSPCDGO = 9 ORDER BY u.PJRQNMBR;
PROMPT ### A.1 Catalogo de caracteristicas de usuario (nombre de cada codigo)
SELECT DISTINCT c.PCXNCDGO, c.PCXNCRTS
  FROM SCP.PCXN c JOIN SCP.PDCN d ON d.PCXNCDGO = c.PCXNCDGO
  JOIN SCP.PJRQ u ON u.PJRQCDGO = d.PJRQCDGO AND u.PGSPCDGO = 9
 ORDER BY c.PCXNCDGO;
PROMPT ### A.1 Caracteristicas de cada usuario (la 30 es la clave: NO se imprime)
SELECT d.PJRQCDGO, u.PJRQNMBR, d.PCXNCDGO, d.PDCNVLRR
  FROM SCP.PDCN d JOIN SCP.PJRQ u ON u.PJRQCDGO = d.PJRQCDGO
 WHERE u.PGSPCDGO = 9 AND d.PCXNCDGO <> 30 ORDER BY u.PJRQNMBR, d.PCXNCDGO;
PROMPT ### A.2 Estructura de las tablas de permisos
SELECT table_name, column_id, column_name, data_type FROM all_tab_columns
 WHERE owner = 'SCP' AND table_name IN ('PENP','PDCN','PCXN') ORDER BY table_name, column_id;
PROMPT ### A.2 Asignaciones de permisos
SELECT pe.*, n.PJRQNMBR AS nodo, n.PGSPCDGO AS tipo_nodo
  FROM SCP.PENP pe LEFT JOIN SCP.PJRQ n ON n.PJRQCDGO = pe.PJRQCDGO ORDER BY pe.PJRQCDGO;
PROMPT ### A.2 Permisos por pantalla cargados (esperado 305)
SELECT COUNT(*) AS permisos_cargados FROM SCP.PJRQ WHERE PGSPCDGO = 11 AND PJRQCDGO BETWEEN 1253 AND 1557;
PROMPT ### A.2 Interruptor del control de permisos (rubro 7, detalle 1)
SELECT r.PRBRDSCR, d.PDTRALTR, d.PDTRDSCR, d.PDTRVLRV, d.PDTRESTD
  FROM SCP.PRBR r JOIN SCP.PDTR d ON d.PRBRCDGO = r.PRBRCDGO
 WHERE r.PRBRALTR = 7 AND d.PDTRALTR = 1;
PROMPT ### A.2 Roles funcionales: aprobadores de pagos y usuarios de caja
SELECT 'APROBADOR CXP' AS rol, x.PJRQCDUS AS codigo_usuario, u.PJRQNMBR AS usuario
  FROM PGS.UXAP x JOIN SCP.PJRQ u ON u.PJRQCDGO = x.PJRQCDUS
UNION ALL
SELECT 'USUARIO DE CAJA', c.PJRQCDUS, u.PJRQNMBR
  FROM TSR.USXC c JOIN SCP.PJRQ u ON u.PJRQCDGO = c.PJRQCDUS
 ORDER BY 1, 3;
PROMPT ### A.3 Usuarios de la app movil por estado (solo conteo, sin datos personales)
SELECT CASE USAPESTD WHEN 1 THEN 'ACTIVO' WHEN 2 THEN 'BLOQUEADO' WHEN 3 THEN 'ELIMINADO' ELSE 'OTRO' END AS estado,
       COUNT(*) AS usuarios, MAX(USAPFCUA) AS ultimo_acceso
  FROM CRD.USAP GROUP BY USAPESTD ORDER BY USAPESTD;
EXIT
"@

$contenedores = @()
if (Get-Command docker -ErrorAction SilentlyContinue) {
    $contenedores = docker ps --format '{{.Names}}|{{.Image}}' | Where-Object { $_ -match 'oracle|database/free|database/enterprise' }
}
if (-not $contenedores) {
    Seccion '5. Bases de datos Oracle' { 'No hay contenedores de Oracle en ejecucion en este servidor.' }
}
foreach ($c in $contenedores) {
    $nombre = ($c -split '\|')[0]
    Seccion "5. Base de datos del contenedor $nombre ($Pdb)" {
        # sqlplus corre en Linux: sin \r, o confunde los comandos
        ($sql -replace "`r", '') | docker exec -i $nombre sqlplus -s -L '/ as sysdba'
    }
}

Write-Host ''
Write-Host "Terminado. Archivo: $salida"
Write-Host 'Revisalo antes de compartirlo y pasaselo al arbitro del equipo 3.'
