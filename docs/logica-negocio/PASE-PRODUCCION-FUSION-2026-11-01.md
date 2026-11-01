# Pase a producción de la versión fusionada (main + cuadre), 2026-11-01

Lo escribió omen-saa-1-arb a pedido del usuario: «fusionar las versiones hoy para que todos los
equipos usen main y subir a producción». Fuente: la entrega del cuadre
(`docs/equipos/cuadre-entrega-para-fusion.md` §3 a §7), contrastada con los scripts en `main`.

## 0. Qué se sube

- **saaBE `main` `2428217a`.** Es la fusión del cuadre (`origin/cuadre` `9ecdd2f2`), sin conflictos,
  más la renumeración de scripts. Compila con `mvn -q compile`.
- **saaFE `main` `72ea63b`.** Es la fusión del cuadre (`9f1cc4b`), sin conflictos. Compila con
  `ng build`.
- **EAR:** `F:\ears\asoprep\fusion\SaaEAR.ear`. Se arma sobre el EAR de producción; solo cambian
  `saaBE.war` y `SaaFE.war`.
- **Lo que trae además de lo del cuadre**, todo lo de esta noche:
  - A1 (afectaciones manuales en AH y en HS huérfano);
  - la carga ARCH;
  - el orden de meses por filial;
  - la glosa con el mes del aporte;
  - la columna «Mes del aporte» y la fecha de caja en el ojo.

## 1. Decisiones del usuario (2026-10-31/11-01)

| Tema | Decisión |
|---|---|
| Comportamiento de cartera | **Como recomienda el cuadre**: `saa.cartera.alCorte=siempre`, `saa.cartera.inicioDevengoMora=2025-05-31`. `saa.cartera.moraNoDevengadaAIngreso` se deja en su valor por defecto, `true`: la mora cobrada no devengada va a ingreso (D-017). |
| Forma del pase | **Directo**. El usuario ya sacó el dump de producción. |
| Regla IN-A/EG-A/DI-A | **Desde el 2025-06-01** (se corre `c12-01`). Desde el pase la contabilidad sale con la numeración nueva, y después se reprocesa la contabilidad hasta noviembre 2026. |
| `CRD.IGFN.IGFNCMNM` | **Pendiente**. No se toca: la G40 no se genera hasta que ASOPREP lo defina. |
| Firmantes de C19-04 | Se corre con los firmantes **en blanco** (el estado financiero se firma a mano), como permite el script. |
| Roles de los permisos 1412, 1494 y 1558-1561 | Los nodos se crean (C17-FE-01). **Asignarlos a roles queda para el usuario** desde la pantalla de permisos. |

## 2. Orden del pase

> **Regla:** cada script trae su control ANTES y DESPUÉS. Si un control ANTES no da lo esperado,
> **parar y avisar**. Correr cada script de corrido en DBeaver (`Alt+X`). En los scripts que no
> traen `COMMIT` (los marcados con «COMMIT a mano»), dar `COMMIT;` después de ver el control
> DESPUÉS. Si DBeaver está en auto-commit, eso ya ocurrió y no hace falta.
> **Nunca correr un script mientras una carga o un proceso largo está corriendo en la aplicación.**

### Paso 1. Respaldo
- ✅ Hecho por el usuario (dump de producción).

### Paso 2. DDL, ANTES de desplegar el EAR

| # | Script (desde `docs/`) | Qué hace | Si falta, con el código nuevo |
|---|---|---|---|
| 2.0 | `logica-negocio/crd/sql/DDL-ANCP-COMPLEMENTO-APERTURA.sql` | `CK_ANCP_TPOO` 1..4 | Ya corrido en producción el 28-10: el control lo confirma; si está, se salta |
| 2.1 | `equipos/cuadre-ddl-produccion-29102026.sql` (bloques 1 y 2) | `CK_DCBC_MDLD` y `UK_ANCP_VIGENTE` | ORA-02290 en el abono sin regenerar tabla |
| 2.2 | `logica-negocio/cnt/sql/C17-01-cierre-contable-y-bitacora.sql` | Columnas de cierre en `CNT.PRDO` y tabla `CNT.HCPR` | **ORA-00904 al leer cualquier período: se cae toda la contabilidad.** Imprescindible |
| 2.3 | `logica-negocio/crd/sql/346_C15_PLANTILLA_37_Y_SUBPROCESOS_8_9_10.sql` (ex-332) | `CK_ANCC_TPOO` 1..10 y plantilla 37 | El cierre de cartera al corte sale sin provisión y da ORA-02290 en las bandas |
| 2.4 | `logica-negocio/rpr/sql/C19-01-ddl-estructuras-de-renglones.sql` | Estructuras de renglones | `/esrp` y `/esfo` responden error |
| 2.5 | `logica-negocio/reportes/sql/336_D035_REGISTRO_SUJETOS_G45.sql` | `RPR.SG45` + semilla | La G45 no se genera |
| 2.6 | `logica-negocio/reportes/sql/337_D038_REGISTRO_G41.sql` | `RPR.SG41` + semilla | La G41 no se genera |
| 2.7 | `logica-negocio/reportes/sql/338_D038_TIPO_PRESTACION_TPAP.sql` | `CRD.TPAP.TPAPTPPR` | ORA-00904 en la consulta de saldos J/C de las G |
| 2.8 | `logica-negocio/cnt/sql/D043-01-asnh-observacion-2000.sql` | `CNT.ASNH.ASNHOBSR` a 2000 | ORA-12899 en la desmayorización |

`reportes/sql/336-338` están en una carpeta distinta de `crd/sql/336-338` (scripts de omen1). Son
archivos distintos: nombrarlos siempre por su ruta completa.

### Paso 3. Propiedades JVM (WildFly), ANTES de arrancar con el EAR nuevo
En la consola de WildFly (`jboss-cli`) o en `standalone.xml` → `<system-properties>`:
```
/system-property=saa.cartera.alCorte:add(value=siempre)
/system-property=saa.cartera.inicioDevengoMora:add(value=2025-05-31)
```
- **NO definir** `saa.cierres.permitirFueraDeOrden`: es solo del reproceso.
- `saa.cartera.moraNoDevengadaAIngreso`, `devengoMoraCierre`, `corteInclusivo` y
  `neteoPorPrestamo` se dejan **sin definir** (valores por defecto).
- Si la propiedad ya existe, `:add` falla. En ese caso usar
  `:write-attribute(name=value,value=...)`.

### Paso 4. EAR
- Desplegar `F:\ears\asoprep\fusion\SaaEAR.ear`. Al terminar, **verificar el hash** del
  `saaBE.war` **y del `SaaFE.war`** desplegados contra los de §5: ya pasó una vez que quedó
  desplegado el WAR viejo.

### Paso 5. Semillas, DESPUÉS del EAR

| # | Script (desde `docs/logica-negocio/`) | Qué hace | Nota |
|---|---|---|---|
| 5.1 | `seguridad/sql/C17-FE-01-permisos-cierres.sql` | Nodos de permiso 1558-1561 | **COMMIT a mano.** CONTROL 1 = 0 filas (el rango libre en toda `SCP.PJRQ`, que es también la tabla de usuarios y empresas). Si no da 0, parar |
| 5.2 | `crd/sql/347_LINEAS_RENDIMIENTOS_PLANTILLA_21.sql` (ex-335) | Plantilla 21: aux1 56 y 57 | **Imprescindible.** Sin él, el asiento de una carga Petro se detiene con un tipo de aporte sin línea |
| 5.3 | `rpr/sql/C19-02-semilla-b17.sql` | Semilla del B17 (534 renglones) | COMMIT a mano |
| 5.4 | `rpr/sql/C19-03-semilla-flujo-de-caja.sql` | Semilla del flujo de caja | COMMIT a mano |
| 5.5 | `rpr/sql/C19-04-estados-financieros-oficiales.sql` | Títulos y rubro 250 (firmantes en blanco) | COMMIT a mano |
| 5.6 | `cnt/sql/c12-01-vigencia-tipos-automaticos.sql` | Rubro 249: vigencia IN-A/EG-A/DI-A = 2025-06-01 para la 1236 | COMMIT a mano. Los controles verifican que PRBR 249 y PDTR 1518 estén libres y que los tipos 7/8/9 tengan `PLNTSSTM = 1`. Si no, parar: sin eso, todo asiento automático falla |
| 5.7 | `cnt/sql/c12-02-bancos-reales.sql` | **Solo SELECT**: verifica la marca de banco real (12 en 1, 6 en 0) | Ya la puso el `e2-111` del equipo 2. Es solo la comprobación |
| 5.8 | `cnt/sql/c5-01-apertura-alterno-10.sql` | `APERTURA = 10` | COMMIT a mano. Correrlo solo si su control muestra que hace falta |

**No van:** `IGFNCMNM` (pendiente), `c11-01` (opcional, solo para el cargador histórico), los
scripts de control del reproceso (`C17-02`, `348`, `349`, `reproceso-carga-352-controles`) y C19-05.

### Paso 6. Arranque y pruebas de humo (con un usuario que NO sea el 1)
1. **Log de WildFly al arrancar:** sin `ORA-00904` y sin `ClassCastException`.
2. **Contabilidad:** abrir Períodos y Cierre de Períodos. Ver un asiento existente y abrir el
   diálogo de cierre de período **sin ejecutarlo**. ⛔ **Nunca crear asientos de prueba en
   producción para anularlos** (regla del usuario): el asiento anulado queda para siempre y gasta
   numeración, que desde este pase es la nueva por módulo.
3. **Cartera:** abrir el detalle de un préstamo y la consulta de cargas Petro (la 492 y la 510 se
   ven procesadas).
4. **Cierres:** correr
   `SELECT CRCTANOO, CRCTMESS, CRCTCDGO FROM CRD.CRCT WHERE PJRQCDGO = 1236 AND CRCTIDST = 2 AND CRCTESTD = 1 ORDER BY 1, 2;`.
   Lo esperado es julio, agosto y septiembre 2026: esos meses quedan **cerrados para cartera**
   (`CARTERA_CERRADA` en cobros o cargas con afectación ahí).
5. **Dashboard del partícipe:** columna «Mes del aporte» y fecha de caja en el ojo.
6. **Plantilla 21, después del 5.2:** el SELECT de control final del
   `crd/sql/347_LINEAS_RENDIMIENTOS_PLANTILLA_21.sql`. Lo esperado: plantilla 21 con aux1 56 →
   2.1.01.05.02 y aux1 57 → 2.1.02.05.02, las dos al HABER. Sin esas líneas, la próxima carga Petro
   se detiene al generar el asiento (`204a7950`).
7. **Sin cobros ni movimientos de prueba en producción.** La regla de la mora no devengada a
   ingreso (D-017) se verá con el primer cobro real.

### Paso 7. Vuelta atrás
- **Si el EAR nuevo falla:** volver a desplegar el EAR anterior (`F:\ears\asoprep\arch-v2\SaaEAR.ear`).
  El DDL del paso 2 **agrega** columnas, tablas y restricciones que el código viejo no lee: no le
  estorba. Las semillas del paso 5 tampoco.
- **Si un script del paso 2 falla a la mitad:** parar, no seguir con el EAR, y pasar el error.
- **El dump** es la última red.

## 3. Cambios de operación que el usuario y contabilidad tienen que saber
- **Cartera cerrada:** los meses con cierre de cartera ejecutado (julio, agosto y septiembre)
  quedan cerrados: cobros, abonos y cargas con fecha ahí responden `CARTERA_CERRADA`. Para tocar
  un mes cerrado hay que reabrirlo, con motivo, desde la pantalla.
- **Períodos contables:** después del C17-01 quedan todos **abiertos**. Se cierran desde la
  pantalla nueva «Cierre de Períodos».
- **Reportes de la Superintendencia (G40-G51):** solo se generan para meses con la cartera
  cerrada. Para las G de octubre, primero el cierre de cartera de octubre.
- **Cierre mensual de cartera:** con `alCorte=siempre`, cada cierre corre provisión, bandas y
  devengo de mora, y genera más asientos que hoy.
- ⛔ **REGLA OBLIGATORIA: el cierre de cartera de octubre 2026 se PREVISUALIZA primero**
  (`POST /cierrecartera/previsualizar` o el botón «Previsualizar» de la pantalla). Es el primer
  cierre en producción con `alCorte=siempre`, y corre por primera vez tres pasos:
  - (8) la provisión lleva la cuenta 1399 de una vez al nivel de la G48;
  - (9) el ajuste de bandas iguala la contabilidad con el módulo por grupo, y la contabilidad de
    cartera de producción nunca cuadró;
  - (10) el devengo de mora reconoce de una vez la mora desde el 2025-05-31.

  Puede generar asientos de ajuste grandes. Son temporales, porque el reproceso definitivo de la
  contabilidad los reemplaza, pero contabilidad los va a ver en los libros.

  **Contabilidad revisa los montos de (8), (9) y (10) por grupo antes de ejecutar.** Si no los
  acepta, se cambia la propiedad sin redesplegar:
  `/system-property=saa.cartera.alCorte:write-attribute(name=value,value=auto)`. Con eso, el
  cierre corre en modo normal hasta el reproceso. La propiedad **no** se cambia ahora (decisión del
  usuario: «siempre»); solo se agrega este control previo.
- **Mora cobrada no devengada:** va a ingreso.
- **Asientos automáticos:** desde el pase salen IN-A/EG-A/DI-A con numeración mensual por módulo.

## 4. Pendientes que este pase no cierra
- IGFNCMNM; firmantes de C19-04; roles de los permisos.
- Reproceso de la contabilidad hasta noviembre 2026 (decisión del usuario, va aparte).
- Defectos abiertos de la carga (`petro/DISENO-AFECTACION-MANUAL-V2.md`,
  `DISENO-CARGA-TARDIA-SIN-MORA.md`, `DISENO-CARGA-RESPUESTA-ARCH.md` §9.5).
- Comentario desactualizado en `CrdLineaAsiento.java` (~163): nombra `335_LINEAS_RENDIMIENTOS_…`,
  que ahora es `347_…`.

## 5. Hashes (se completan al armar el EAR)
- EAR `F:\ears\asoprep\fusion\SaaEAR.ear`: SHA-256 `3BC673DD83125A5B7DCEB558ED55DED141ED200DC753475F066CFC2CB24765CF`
- saaBE.war (dentro del EAR): SHA-256 `A2B76F451AFD4049CF5406B19661F35FBF7BB48F7962AA7DA9921CC3B18DB975`
- SaaFE.war (dentro del EAR): SHA-256 `A782932649DDED0DD4178B6500C4D751D0A0072257F9C4B2AAC3DE2B72565065`

## 6. Qué mandarle al agente del cuadre después del pase
- Los hashes desplegados de saaBE.war y SaaFE.war, confirmando que coinciden con §5.
- Cada script corrido, en orden, con la salida de sus controles ANTES y DESPUÉS, y cuáles se
  saltaron (por ejemplo 2.0, si `CK_ANCP_TPOO` ya estaba, o 5.8, si APERTURA no hacía falta).
- Las propiedades JVM aplicadas: salida de `/system-property=saa.cartera.alCorte:read-resource` y
  de `/system-property=saa.cartera.inicioDevengoMora:read-resource`.
- El resultado del humo, incluido el SELECT de `CRD.CRCT` con los meses cerrados para cartera.
- Cualquier error del log de WildFly al arrancar.
- Verificado dentro del WAR: `ReprocesoCargaPetroServiceImpl` y `CierrePeriodoContableServiceImpl`
  (cuadre), A1, el lector de ARCH y la glosa «pagado en» (main).
- Vuelta atrás: `F:\ears\asoprep\arch-v2\SaaEAR.ear` (el desplegado la noche del 31-10).
- Para verificar el hash del WAR desplegado en el servidor (PowerShell):
  `Get-FileHash <ruta del saaBE.war desplegado> -Algorithm SHA256`
