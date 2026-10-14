# Copiar producción a las bases de pruebas (.2.4 CUADRE y .2.3 PRUEBAS)

Producción **solo se lee**: está en el `.2.4`, contenedor `saa-oracle-23ai`, puerto 1521. Todo lo que se
borra o se escribe ocurre en el contenedor **`saa-pruebas`** (puerto 1531) del servidor donde se corre.

| Servidor | Copia desde | Nombre de la empresa | WildFly de pruebas |
|---|---|---|---|
| `192.168.2.4` | `host.docker.internal:1521` (el mismo servidor) | `ASOPREP CUADRE` | `D:\wildfly38-pruebas`, 8180 |
| `192.168.2.3` | `192.168.2.4:1521` (por la red) | `ASOPREP PRUEBAS` | `D:\wildfly38-pruebas`, 8180, `-b 0.0.0.0` |

Hacer **un servidor a la vez**: cada copia lee producción entera, unos 12 minutos. Mejor fuera de horario.
Todo en PowerShell como administrador, desde `D:\saa-pruebas`.

## 1. Detener el WildFly de pruebas

En su consola: **Ctrl+C**. Luego comprobar que quedó abajo (tiene que salir vacío):

```powershell
netstat -ano | findstr "LISTENING" | findstr ":8180 :8543 :10090"
```

## 2. Confirmar que se está en pruebas

```powershell
cd D:\saa-pruebas
docker port saa-pruebas
docker exec saa-pruebas printenv PROD_HOST
```

Tiene que salir `1521/tcp -> 0.0.0.0:1531`, y `host.docker.internal` en el `.2.4` o `192.168.2.4` en el `.2.3`.

## 3. Vaciar la base de pruebas

```powershell
Get-Content .\sql\borrar-esquemas-pruebas.sql | docker exec -i saa-pruebas bash -c 'sqlplus -s -l "system/$ORACLE_PWD@localhost:1521/FREEPDB1"'
```

Tiene que salir `base de pruebas confirmada: ASOPREP CUADRE` (o `PRUEBAS`), una línea `borrado:` por esquema,
y la lista **DESPUÉS** vacía. Si sale `ORA-20010` u `ORA-20011`, **no es la base de pruebas: parar.**

## 4. Copiar producción

```powershell
$s = Read-Host 'Clave de SYSTEM de produccion (.4)' -AsSecureString
$env:PROD_SYSTEM_PWD = [Runtime.InteropServices.Marshal]::PtrToStringAuto([Runtime.InteropServices.Marshal]::SecureStringToBSTR($s))
docker exec -e PROD_SYSTEM_PWD saa-pruebas bash /opt/oracle/scripts/setup/01-clonar-produccion.sh
$env:PROD_SYSTEM_PWD = $null
```

Resumen del resultado:

```powershell
docker exec saa-pruebas bash -c "tail -400 /opt/oracle/oradata/clon_produccion.log | grep -E 'Esquemas a copiar|completed|ORA-39|ORA-3168|estadisticas OK|prod=|invalidos|facturadores|TERMINADO|No se clona|no se pudo'"
```

| Tiene que salir | Si no |
|---|---|
| `PRST / APRT / ASNT / DTAS ... OK` | `DIFERENTE` por pocas filas en ASNT/DTAS: alguien trabajó en producción durante la copia |
| `facturadores en ambiente SRI de PRODUCCION que quedan: 0` | **no abrir el ambiente**; avisar |
| `estadisticas OK:` por cada esquema | — |
| `CLON DE PRODUCCION TERMINADO` | — |
| (no debe salir) `ya tiene N esquema(s). No se clona encima` | falta el paso 3 |
| (no debe salir) `no se pudo conectar a producción` | clave mal escrita; repetir el paso 4 |

Los `ORA-20000: Unable to set values for index` y `completed with N error(s)` son normales: son las
estadísticas de los índices, que el paso de estadísticas vuelve a calcular.

## 5. Nombre de la empresa

En el `.2.4`:

```powershell
@("define nuevo_nombre = 'ASOPREP CUADRE'") + (Get-Content .\sql\renombrar-empresa-pruebas.sql) | docker exec -i saa-pruebas bash -c 'sqlplus -s -l "system/$ORACLE_PWD@localhost:1521/FREEPDB1"'
```

En el `.2.3`:

```powershell
@("define nuevo_nombre = 'ASOPREP PRUEBAS'") + (Get-Content .\sql\renombrar-empresa-pruebas.sql) | docker exec -i saa-pruebas bash -c 'sqlplus -s -l "system/$ORACLE_PWD@localhost:1521/FREEPDB1"'
```

Tiene que salir `filas cambiadas: 1  ->  ASOPREP ...`.

## 6. Arrancar el WildFly de pruebas

En una consola aparte que queda abierta:

- `.2.4`: `D:\wildfly38-pruebas\bin\standalone.bat`
- `.2.3`: `D:\wildfly38-pruebas\bin\standalone.bat -b 0.0.0.0`

En el log: el listener en **8180** (si dice 8080, Ctrl+C), `Bound data source [java:/jdbc/SaaDS]`,
`127.0.0.1:10090/management` y `WFLYSRV0025`.

## 7. Comprobar a qué base va cada WildFly

```powershell
Get-NetTCPConnection -State Established -RemotePort 1521,1531 -ErrorAction SilentlyContinue |
  ForEach-Object { $p = Get-CimInstance Win32_Process -Filter "ProcessId=$($_.OwningProcess)"; [pscustomobject]@{Puerto=$_.RemotePort; PID=$_.OwningProcess; Ruta=($p.CommandLine -replace '.*-Djboss.home.dir=("?)([^" ]+).*','$2')} } |
  Sort-Object Puerto, PID -Unique | Format-Table -AutoSize
```

`D:\wildfly38-pruebas` tiene que salir **solo con 1531**. Luego salir del SAA, cerrar todas las
ventanas de incógnito y entrar de nuevo: arriba tiene que salir el nombre del paso 5.

## Archivos que tiene que tener `D:\saa-pruebas` en cada servidor

`docker-compose.yml`, `.env` (con `PROD_SYSTEM_PWD=` **vacío**; en el `.2.3` además `PROD_HOST=192.168.2.4`),
`setup\01-clonar-produccion.sh` (con saltos de línea LF), `sql\borrar-esquemas-pruebas.sql`,
`sql\renombrar-empresa-pruebas.sql`. Al cambiar el `.sh` no hace falta reiniciar el contenedor: la
carpeta `setup` está montada.
