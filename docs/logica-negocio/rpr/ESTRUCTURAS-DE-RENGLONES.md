# Reportes de renglones propios en el motor de balances (C19 / C19b) — B17 y flujo de caja como configuración

**2026-10-28 (C19) · 2026-10-29 (C19b, decisión D-031 del líder: sin motor paralelo).** Inventario de reportes
en [REPORTES-SUPERINTENDENCIA.md](REPORTES-SUPERINTENDENCIA.md).

## 1. Problema y decisión

El motor de balances (`POST /dtmt/generarBalance` → `TempReportesServiceImpl.reporteRangoFecha`, definición en
CNT.RPRT + CNT.DTRP, resultado en CNT.DTMT) copia **rangos del plan de cuentas**: cada renglón ES una cuenta del
plan y el padre se arrastra por `PLNNCDPD`. El B17 no: sus renglones son el catálogo de la Superintendencia (534
códigos sin puntos, `110105`), cada uno alimentado por varias cuentas con reglas propias (el "7" es neto deudor,
las 7.x van en valor absoluto, 32 cuentas "REQUIERE ASIGNACIÓN" van a un renglón decidido a mano), y sale en un
`.txt` de formato exacto. El flujo de caja igual (renglones del formato BIESS, totales por fórmula, saldo inicial a
otra fecha).

La primera versión de C19 (28-oct) lo resolvió con un motor aparte (`/esrp`, tabla `CNT.CRRP`). **D-031 (29-oct):
no se acepta un motor paralelo.** C19b lo integra al motor existente: mismas tablas de definición, mismo endpoint,
mismo resultado en DTMT.

## 2. Modelo (todo cuelga del reporte del motor)

| Tabla | Qué se agregó |
|---|---|
| `CNT.RPRT` (cabecera, `ReporteContable`) | 11 columnas opcionales: `RPRTTPRP` tipo (NULL/1 balance sobre el plan · **2 renglones propios**) · `RPRTCDES` código de estructura (B17) · `RPRTPRDC` periodicidad (M) · `RPRTCDEN` entidad (3968) · `RPRTBSVL` base del valor (1 saldo al corte · 2 movimiento del período · 3 saldo al inicio) · `RPRTMPAU` mapeo automático por prefijo · `RPRTACPF` acumulación por prefijo de código · `RPRTFRAR` **tipo de salida en archivo** (1 = TXT de la Superintendencia) · `RPRTSMCT` suma de control · `RPRTTTLO` título oficial · `RPRTNVMX` **nivel de profundidad** (dígitos máximos; también para los balances de siempre) |
| `CNT.DTRP` (detalle, `DetalleReporteContable`) | 7 columnas opcionales. En un reporte de renglones cada fila es una **regla cuenta → renglón**: `NUMCTADS`/`PLNNCDDS` = la cuenta del plan, `DTRPCDRN` renglón destino (NULL = mapeo automático), `DTRPALCN` alcance (1 con subcuentas · 2 exacta), `DTRPMDSG` signo (1 naturaleza · 2 deudor · 3 acreedor · 4 valor absoluto por cuenta), `DTRPRPRT` se reporta (0 = excluida a propósito), `DTRPFCDS`/`DTRPFCHS` vigencia, `DTRPOBSR` sustento. En los balances de siempre quedan NULL. |
| `CNT.RNRP` (detalle nuevo, `RenglonReporteContable`, CRUD `/rnrp`) | Los renglones del reporte: código, nombre, nivel, orden, tipo de cálculo (1 suma · 2 neto deudor · 3 fórmula `+A -B`), base propia, ¿va al archivo?, vigencia, estado. **Es la única tabla nueva y es estrictamente necesaria**: los 534 códigos del B17 no son cuentas del plan, no caben en PLNN ni en DTRP (que son reglas sobre cuentas). |

`CNT.CRRP` (motor paralelo) se retira: sus reglas pasan a DTRP (C19-05) y la tabla se borra.

## 3. Cálculo (dentro de `TempReportesServiceImpl`)

`reporteRangoFecha` mira el tipo del reporte: **NULL/1 → exactamente el código de siempre** (el diff solo agrega
líneas: la consulta del reporte y el corte por `RPRTNVMX`, que con NULL no hace nada); **2 → `calculaRenglones` +
`reporteRenglones`**. Por centro de costo y distribuido rechazan un reporte de renglones.

`calculaRenglones` usa `com.saa.ejb.cnt.estructura.CalculadorEstructura` (clase pura, la misma validada contra el
B17 presentado):
1. Debe/haber por cuenta (`TempReportesDaoService.selectSaldosPorCuenta`, asientos ACTIVO y REVERSADO como el
   resto del motor) por cada base necesaria.
2. Cada cuenta se resuelve con la regla de DTRP de cuenta **más larga** que la alcance (varias reglas con la misma
   cuenta = varios destinos); sin regla o sin destino, el mapeo automático (`1.1.01.05.01` → `110105`).
3. Aporte según el signo de la regla; además el neto deudor para los renglones tipo 2.
4. Renglón tipo 1 = suma de los destinos que empiezan por su código (`RPRTACPF = 1`) o del suyo; tipo 2 = neto
   deudor; tipo 3 = fórmula. BigDecimal exacto, redondeo a 2 decimales por renglón.
5. Comparativo opcional (`fechaComparacion`).
6. Si el reporte tiene salida en archivo (`RPRTFRAR = 1`): nombre `B17M3968ddMMaaaa.txt` y contenido (cabecera
   `B17 TAB 3968 TAB dd/MM/aaaa TAB líneas TAB suma`, `código TAB valor`, `0` si es cero, CRLF sin salto final,
   ISO-8859-1), igual que `09-genera-b17-desde-saa.js`.

`reporteRenglones` deja el resultado en **CNT.DTMT** como cualquier ejecución: `DTMTCTCN` = código del renglón,
`PLNNNMBR`/`PLNNNVLL` = nombre/nivel, `DTMTSLAC` = valor, `DTMTSLAN` = valor comparativo (si se pidió), debe/haber
0. Por eso se ve en la pantalla de Balance General y se borra con `DELETE /dtmt/resultado/{id}` como siempre.

## 4. Mes cerrado (C17) y modo preliminar

Los reportes con archivo para la Superintendencia exigen el mes **cerrado para cartera y contabilidad**
(`EstadoCierreService.validarReporteFinanciero`, mensaje `REPORTE_MES_NO_CERRADO`, HTTP 400). Para el re-proceso:
`"preliminar": true` → genera sin cierre, la respuesta lo marca y el archivo se llama `…_PRELIMINAR.txt`.

## 5. Endpoints (todos del motor, `/dtmt`)

- `POST /dtmt/generarBalance` — igual que siempre. Con un reporte de renglones además responde `sumaControl`,
  `advertencias` (cuentas con saldo sin renglón o excluidas), `nombreArchivo`, `preliminar`.
- `POST /dtmt/generarArchivo` — mismo cuerpo; devuelve el `.txt` (Content-Disposition con el nombre).
- `GET /dtmt/resultado/{id}`, `DELETE /dtmt/resultado/{id}` — sin cambios.
- `POST /dtmt/estadoOficial/{encabezado|datos|imprimir}` — estados financieros oficiales (antes `/esfo`).
- `/rprt`, `/dtrp` (CRUD de siempre, ahora con los campos nuevos) y `/rnrp` (CRUD de renglones).
- Cuerpo nuevo opcional de `ParametrosBalance`: `fechaComparacion`, `fechaInicioComparacion`, `preliminar` y
  `codigoEstructura` (elige el reporte por su estructura, p. ej. `B17`, cuando no viene `codigoAlterno`).
- **Alias conservados** (rebase sobre `main` del 24-nov-2026): el frontend de `main` llama `/esrp/generar`,
  `/esrp/archivo` (reportes del mes, C36) y `/esfo/*` (balance general y reportes del mes). Se mantienen con el mismo
  contrato pero ya no hay motor paralelo: `/esrp` traduce su cuerpo (`SolicitudEstructura`) a `ParametrosBalance` y
  calcula con `TempReportesService.calculaRenglones`; `/esfo` y `/dtmt/estadoOficial` llaman al mismo
  `EstadoFinancieroOficialService`. Se retiran cuando el frontend pase a `/dtmt`. **Retirado:** `EstructuraReporteService`.

### 5.1 Lo que `main` construyó sobre el servicio retirado y ahora vive en el motor

- **C39 (D-150), saldos negativos del B17:** `calculaRenglones` agrega a las advertencias de todo reporte con archivo
  de la Superintendencia un aviso `SALDO NEGATIVO (la Superintendencia lo rechaza): cuenta … = …` por cada renglón que
  va al archivo con valor negativo, salvo las correctoras (`1x99…`, `1xxx99…`) y las de orden (`7`)
  (`TempReportesServiceImpl.agregaAvisosSaldoNegativo`). `ControlCierreServiceImpl.controlSaldosNegativosB17` arma el
  B17 preliminar del mes con `calculaRenglones` (`codigoEstructura = B17`, sin escribir en `CNT.DTMT`) y cuenta esos avisos.
- **C38, flujo de efectivo y comparativo:** `FlujoEfectivoServiceImpl` toma del motor la definición
  (`definicionRenglones`), los renglones (`renglonesVigentes`), las reglas de `CNT.DTRP` (`reglasVigentes`), el B17
  (`calculaRenglones`) y los saldos por cuenta (`TempReportesDaoService.selectSaldosPorCuenta`). `calculaRenglones`
  rechaza los formatos derivados del B17 (`RPRTFRAR = 2`: FLE, FCD, CMP), que solo se generan con `/flef`.

## 6. Definiciones y scripts (`sql/`)

| Script | Para qué | Dónde |
|---|---|---|
| `C19-01` | DDL final: columnas de RPRT y DTRP + tabla RNRP | ambiente limpio (producción) |
| `C19-02` | B17 (alterno 6): cabecera, 534 renglones, 33 reglas en DTRP. Generado por `C19-generar-semilla-b17.js` | ambiente limpio |
| `C19-03` | Flujo de caja (alterno 7, provisional): 26 renglones, 12 reglas en DTRP | ambiente limpio |
| `C19-04` | Títulos oficiales y rubro 250 | los dos |
| `C19-05` | **Migración** CRRP → DTRP (+ columnas nuevas) y retiro de CRRP. Genérica: copia toda regla activa (al 24-nov-2026 en la base de trabajo: B17 35, FLC 12, FCD 110 de C38) | toda base donde exista `CNT.CRRP` (la de trabajo; producción si allí corrió el C19-01 de `main`) |
| `C19-06` | Comparación antes/después de los balances de siempre | después de desplegar |
| `C19-07` | Ambiente limpio: las reglas que `main` cargó en CRRP después de C19-02 (D-097 (7) 4.8.99.01–07 al 489090, D-053 4.8.99.08, D-180 4.8.99.9) y las 110 de FCD de C38-01, en DTRP. Generado por `C19-07-generar.js` desde C38-01 | ambiente limpio, después de C38-01 **sin su BLOQUE 5 ni el 7** |

## 7. Validación (base de trabajo `saa-oracle-23ai`, 2026-10-29)

Harness fuera del servidor: lee la definición **de los INSERT de C19-02/C19-03** (renglones RNRP y reglas DTRP,
con el mismo mapeo DTRP → regla que hace el motor), saldos por cuenta con la misma consulta del DAO, corre
`CalculadorEstructura` + `ArchivoEstructura` y compara bytes.

| Prueba | Resultado |
|---|---|
| B17 31/05/2025 | **Idéntico byte a byte** a `B17M396831052025.txt` presentado (6.534 bytes, suma 900.189.987,57) |
| B17 30/06/2025 | Idéntico al producido por la lógica del script 09 (6.612 bytes) |
| B17 30/09/2026 | Difiere del script 09 solo en `4`, `48` y la suma: la cuenta nueva 4.8.02.1 (12.751,99) entra por prefijo a `48`; el script la descartaba. Decidir su renglón |
| Flujo sep-2026 | Igual que antes de la migración (FC91 = −900.998,65 por las reglas de caja que faltan) |
| Migración C19-05 | Las 45 reglas de CNT.CRRP de la base = las 45 reglas DTRP de C19-02 + C19-03 (comparación campo a campo), todas con cuenta del plan |
| Balances de siempre | El diff de `TempReportesServiceImpl` solo agrega líneas; la comparación de ejecuciones antes/después (C19-06) requiere desplegar y **está pendiente** |
| C19-07 (24-nov) | Sus 110 reglas de FCD = las 110 de `CNT.CRRP` de FCD en la base (cuenta, alcance, renglón, signo, se reporta, vigencias y sustento) |
