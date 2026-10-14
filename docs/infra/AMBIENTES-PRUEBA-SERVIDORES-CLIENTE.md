# Ambientes de prueba en los servidores del cliente (`.2.4` y `.2.3`)

**Autor:** `omen-saa-3-arb` · **Fecha:** 2026-10-13 · **Estado:** diseño, nada ejecutado todavía.

## Qué se quiere

Un SAA de pruebas en cada servidor del cliente. Cada uno corre sobre su propia base Oracle en Docker,
idéntica a la de producción:

| Servidor | Hoy tiene | Se agrega |
|---|---|---|
| `192.168.2.4` (**PRODUCCIÓN**) | Docker con la base de producción, WildFly en `D:` | contenedor `saa-pruebas` + WildFly de pruebas en `D:` |
| `192.168.2.3` | Docker, WildFly en `D:` | contenedor `saa-pruebas` + WildFly de pruebas en `D:` |

## Regla de oro

En `.2.4`, **nada del ambiente de pruebas puede tocar lo de producción**:

- ni el contenedor ni el volumen de la base de producción;
- ni el WildFly de producción, ni sus puertos;
- ni el directorio de archivos subidos (`saa.upload.dir`).

Todo se crea con nombres, puertos y carpetas nuevas. **Primero se monta en `.2.3`** y, cuando funcione,
se repite en `.2.4`.

## ⛔ Lo que un clon de producción haría «de verdad» (verificado en el código el 2026-10-13)

Un clon exacto trae la configuración de producción, y el sistema actúa hacia afuera. Antes de abrir el
ambiente a usuarios, hay que neutralizar en **la base de pruebas** (nunca en la de producción):

1. **Facturación electrónica al SRI de producción.**
   - El ambiente (1 = pruebas `celcer`, 2 = producción `cel`) sale del facturador en la base
     (`FacturaServiceImpl:164-168, 436`), igual que el indicador `conectaSRI` (`:399`).
   - Con datos de producción, **una factura emitida en pruebas se envía al SRI real** y gasta un
     secuencial real.
   - En la base de pruebas: poner el facturador en ambiente 1 o `conectaSRI = 0`.
2. **Correos a clientes reales.**
   - `EmailFacturaServiceImpl` envía desde `facturacion@asoprep.com.ec` por `smtp.office365.com:587`, con
     la cuenta escrita en el código (`:32`).
   - No hay cómo apagarlo por configuración. Opciones:
     - bloquear la salida al puerto 587 para el WildFly de pruebas (firewall de Windows por programa);
     - o cambiar en la base de pruebas los correos de los clientes por uno de prueba.
3. **El timer de mora corre todos los días a las 02:00** (`ProcesoMoraPrestamoTimer:71`) en cada
   WildFly que tenga el WAR.
   - Sobre la base de pruebas no daña nada.
   - Sí consume CPU del servidor de producción a la misma hora que el de producción.
4. **Archivos subidos.** Si el WildFly de pruebas no recibe su propio `-Dsaa.upload.dir`, escribe en la
   misma ruta por defecto que producción (`FileServiceImpl`).

## Pasos por servidor

Las variables van entre `<>`. Se averiguan en el paso 0.

### 0. Reconocimiento (solo lectura)

```powershell
docker ps -a --format "{{.Names}} | {{.Image}} | {{.Ports}}"
docker volume ls
docker exec <contenedor-prod> bash -c "lsnrctl status | grep -i service"   # nombre del PDB
Get-ChildItem D:\ | Where-Object Name -like "*wildfly*"
Get-PSDrive C,D                                                             # espacio libre
```

- Anotar la imagen de Oracle de producción (la de pruebas tiene que ser **la misma versión**).
- Anotar el PDB, el tamaño de los datafiles y los puertos ocupados.

> Si producción es Oracle **Free**, la base tiene un límite de 12 GB de datos de usuario. La copia
> también, así que entra.

### 1. Exportar producción (en línea, sin detenerla)

Data Pump, con una imagen consistente del momento (`FLASHBACK_TIME`). El esquema `SAA_OWNER` es un
ejemplo: hay que exportar todos los esquemas del SAA (`CRD`, `TSR`, `PGS`, `RHH`, `CBR`, `RPR`, `CNT`,
`SCP`, `AOT` y los que aparezcan en `DBA_USERS` con `ORACLE_MAINTAINED = 'N'`).

```bash
docker exec <contenedor-prod> bash -c "mkdir -p /opt/oracle/oradata/dump"
# crear el DIRECTORY una sola vez como SYS: CREATE DIRECTORY DP_DIR AS '/opt/oracle/oradata/dump';
docker exec <contenedor-prod> bash -c "expdp system/<pwd>@<PDB> schemas=CRD,TSR,PGS,RHH,CBR,RPR,CNT,SCP,AOT \
   directory=DP_DIR dumpfile=saa_%U.dmp logfile=exp_saa.log parallel=2 flashback_time=systimestamp"
docker cp <contenedor-prod>:/opt/oracle/oradata/dump D:\saa-pruebas\dump
```

### 2. Contenedor de pruebas

```powershell
docker run -d --name saa-pruebas -p 1531:1521 `
  -e ORACLE_PWD=<pwd-pruebas> -v saa-pruebas-data:/opt/oracle/oradata `
  --memory 4g <misma-imagen-que-prod>
```

- Puerto **1531**, no 1521.
- Volumen nuevo, nunca el de producción.
- Contraseña distinta de la de producción.

### 3. Importar

```bash
docker cp D:\saa-pruebas\dump saa-pruebas:/opt/oracle/oradata/dump
# como SYS en el PDB de pruebas: CREATE DIRECTORY DP_DIR AS '/opt/oracle/oradata/dump';
docker exec saa-pruebas bash -c "impdp system/<pwd-pruebas>@FREEPDB1 directory=DP_DIR dumpfile=saa_%U.dmp logfile=imp_saa.log parallel=2"
```

- Control: el conteo de objetos por esquema (`DBA_OBJECTS`) y de filas de 5 tablas clave (`CRD.PRST`,
  `CRD.APRT`, `CNT.ASNT`, `CNT.DTAS`, `TSR` de extractos) debe ser igual en las dos bases.
- Después del import se corre el **script de neutralización** (SRI y correos) **solo en pruebas**.

### 4. WildFly de pruebas

1. Copiar la instalación: `D:\<wildfly-prod>` → `D:\wildfly-pruebas`.
   - Se copia sin `standalone\data`, `standalone\tmp`, `standalone\log` ni `standalone\deployments\*`.
2. En `D:\wildfly-pruebas\standalone\configuration\standalone.xml`, el datasource `java:/jdbc/SaaDS`
   apunta a `jdbc:oracle:thin:@localhost:1531/FREEPDB1`, con el usuario y la contraseña de pruebas.
   - El nombre JNDI se queda igual: la aplicación lo busca así.
3. Arranque con desplazamiento de puertos y carpeta de archivos propia:

   ```powershell
   D:\wildfly-pruebas\bin\standalone.bat -Djboss.socket.binding.port-offset=100 `
     -Dsaa.upload.dir=D:\saa-pruebas\uploads\
   ```

   - Puertos que quedan: HTTP **8180**, gestión **10090**.
   - Si el WildFly de producción ya usa un desplazamiento, se elige otro que no choque.
4. Desplegar el mismo `SaaBE.war` y el mismo frontend que producción.
   - El frontend usa la ruta relativa `/SaaBE/rest` (`environment.prod.ts`), así que funciona
     servido desde el WildFly de pruebas sin recompilar.
5. CORS: correr `config/standalone-cors.cli` contra el puerto de gestión **10090**.
6. Abrir el puerto 8180 en el firewall de Windows solo para la red interna.
7. Bloquear la salida al 587, o aplicar la opción equivalente del punto 2 de la sección ⛔.

### 5. Diferenciarlo a la vista

Los usuarios van a confundir pruebas con producción.

- Lo mínimo: cambiar el nombre de la empresa en la base de pruebas, por ejemplo con el sufijo «— PRUEBAS».
- Ideal (FE, otro día): una franja de color cuando el host o el puerto no son los de producción.

## Refrescar la copia

Se repiten los pasos 1, 3 y la neutralización: borrar los esquemas en pruebas y volver a importar.
Conviene un `.bat` por servidor cuando el procedimiento quede probado.
