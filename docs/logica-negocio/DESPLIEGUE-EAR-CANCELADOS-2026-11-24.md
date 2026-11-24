# Despliegue del EAR «cancelados-1124» — todo `main` (2026-11-24)

**omen-saa-1-arb.** Pedido del usuario: «commit y push al main para manejar una sola versión con todos» y el EAR para
producción. Es **todo `main`** de los dos repos al 2026-11-24, armado sobre el EAR desplegado
`precancelar-vencido-1118` (`685F6C54…F68748F`, verificado antes de armar).

## 1. El EAR
- `F:\ears\asoprep\cancelados-1124\SaaEAR.ear` — SHA-256 `70131F0C9497363AE8CFE6304D42CE747CA85E83DADD16367C444BFA5D581DA6`
  (62.426.053 bytes; 3 partes en `cancelados-1124\partes`; unir con
  `copy /b SaaEAR.ear.part00+SaaEAR.ear.part01+SaaEAR.ear.part02 SaaEAR.ear`). `unzip -t`: sin errores.
  - `saaBE.war` `FC63AD63F854F02FECD1E523BCE4849D096A583878C3F7DE695DCDEFB0100FFD` — saaBE `main` en `c9147164`
    (`mvn -o package` sobre `git archive` limpio: **no** lleva el cambio de USAP de `app1` que está sin commitear en el árbol).
  - `SaaFE.war` `69B9403C90E083351EA83E0AFB09987BC8A3FC134EE7DCF697E863A684276410` — saaFE `main` en `63a225d`
    (`ng build --configuration production`; `WEB-INF/web.xml` y `META-INF/MANIFEST.MF` del SaaFE.war de `precancelar-vencido-1118`).
  - El resto del EAR (`application.xml`, `assets/`) es copia byte a byte del anterior.
- **Vuelta atrás:** `F:\ears\asoprep\precancelar-vencido-1118\SaaEAR.ear` (`685F6C54…F68748F`). Los scripts del §2 pueden
  quedarse: son aditivos (columnas y tablas nuevas, un NOT NULL relajado, catálogos) y el EAR viejo no los lee.

## 2. ⛔ ANTES del EAR — scripts que `main` necesita y que NO tenían constancia de haberse corrido en producción

Las entidades del EAR mapean columnas que crean estos scripts. **Si falta alguno de los C34, Seguimiento de Novedades
(v1 y v2) revienta con ORA-00904** al abrirse: `ADNV`, `NVDD`, `RQNV` y `RSNV` son tablas de la v1 (C25) que ya existen y
ganan columnas. Correr en este orden y bloque por bloque (cada uno trae sus controles):

| # | Script | Qué hace | Si falta |
|---|---|---|---|
| 1 | `cnt/sql/C34-01-novedades-v2.sql` | tablas `CNT.LNNV`, `CNT.JSNV`, `CNT.RPNV`, `CNT.DONV`; columnas en `ADNV`, `NVDD`, `RQNV`, `RSNV`; checks de `ADNV`/`HSNV` | ORA-00904 en Seguimiento Novedades |
| 2 | `cnt/sql/C34-01b-ck-adnv-dueno-justificacion.sql` | amplía `CK_ADNV_DUENO` | adjunto de una justificación rechazado |
| 3 | `cnt/sql/C34-07-hechos-credito.sql` | tablas `CNT.HCNV`, `CNT.OHNV`; columnas en `ADNV`, `JSNV` | ORA-00904 |
| 4 | `cnt/sql/C34-07b-hechos-serie-y-notas.sql` | tabla `CNT.NTNV`; columnas en `HCNV` | ORA-00904 |
| 5 | `cnt/sql/C34-07c-diferencia-cruce-aportes.sql` | columna `HCNV.HCNVTPAP` | ORA-00904 |
| 6 | `cnt/sql/C34-07d-diferencia-condonacion.sql` | columnas `HCNV.HCNVCNDN`, `HCNVDCSN` | ORA-00904 |
| 7 | `crd/sql/380_C35_HDTP_EVENTO_OPCIONAL.sql` | `CRD.HDTP.EVPRCDGO` admite NULL | ORA-01400 al historizar una cuota sin evento (C35) |
| 8 | `sri/sql/C37-01-rubro-centros-credito-tributario.sql` | rubro y detalle de centros de crédito tributario | sustento por centro de costo sin catálogo |
| 9 | `rpr/sql/C38-01-semilla-flujo-efectivo-comparativo.sql` | semilla de los formatos de flujo de efectivo | los reportes C38 salen vacíos |

Son del **cuadre** (C34–C38), no de `omen-saa-1`. Si ya se corrieron en producción con la corrida definitiva, saltar.

## 3. Qué trae respecto de `precancelar-vencido-1118`

| Cambio | Commits | Equipo |
|---|---|---|
| **Reporte de préstamos cancelados** por rango: pantalla (Créditos › Préstamos › «Préstamos Cancelados»), PDF agrupado por forma y CSV en columnas. Solo lectura | BE `38c2536c`, `c9147164`; FE `d3f2b15` | omen1 |
| ⚠️ **C33/C33b/C33c — prelación de cuotas en los pagos**: no se aplica dinero a una cuota si una anterior tiene saldo; la carga Petro elige la cuota por saldo al corte; la precancelación cobra completa una cuota vencida con saldo real (D-141); la re-imputación automática queda **apagada por defecto** (D-130) | BE `d10c0cb0`, `d61df89d`, `0ea9b2e6`, `aae03562` | cuadre |
| ⚠️ **C35** — `DTPRSLOT` = lo respaldado por los pagos en precancelación, abono, reapertura y recálculo; HDTP sin evento | BE `a2032e37`, `ced00e31` (+ sql 380) | cuadre |
| **C34–C34g** — Seguimiento Novedades v2 (Contabilidad › Procesos), hechos de crédito, regularización con asiento | BE `8d91e497`…`fcf9a660`; FE `017e145`…`6bc05d8` (+ sql C34) | cuadre |
| C27-b2 — al corte, las cuotas eliminadas por un abono posterior se bandean con su fecha | BE `b6cf8a44` | cuadre |
| C36/C38 — reportes del mes para la Super en una pantalla; flujo de efectivo, flujo BIESS y comparativo | BE `2bab1ae2`; FE `7fa82b1`, `48018dc` (+ sql C38) | cuadre |
| C37 — sustento tributario de compras por centro de costo | BE `96d9d1da` (+ sql C37) | cuadre |
| C39 — terceros de las planillas eléctricas en el cargador histórico C11 | BE `9104efdc` | cuadre |
| Ficha del partícipe: la ciudad es combo con buscador (laptop1, no había entrado en el EAR anterior) | FE `5f6cf08` | laptop1 |
| app1: solo documentos (orden y medición de USAP). **El cambio de código de `desactivar` NO está** (sin commitear) | BE `121cf8fa`, `d3fa5a40` | app1 |

## 4. Despliegue y humo (solo lectura; sin pagos ni asientos de prueba)
1. `Get-FileHash F:\ears\asoprep\cancelados-1124\SaaEAR.ear -Algorithm SHA256` = `70131F0C…5D581DA6`. Desplegar.
   «Last enabled at»; log del arranque sin `ERROR` ni `ORA-`.
2. **Préstamos Cancelados:** consultar el mes anterior con los cuatro estados y sin filtros. Correr el bloque 1 de
   `crd/sql/381_MEDICION_PRESTAMOS_CANCELADOS.sql` con ese rango: el TOTAL = «cantidad» de la pantalla. Abrir el detalle
   de un préstamo de cada forma que aparezca. Descargar el PDF y el CSV (abrirlo en Excel: autofiltro y montos como número).
3. **Seguimiento Novedades** (v1 y v2): abrir las dos pantallas. Si dan ORA-00904, falta un script del §2.
4. **Pagos (C33):** no se prueba con dinero. El primer cobro, la primera carga Petro y la primera precancelación reales
   después del despliegue se revisan: cuota afectada en orden y sin cuota anterior con saldo.

## 5. Riesgos
- **El reporte de cancelados nunca corrió contra Oracle** (compila, el PDF se llenó con filas de prueba): el punto 2 del
  humo es su primera ejecución real. Es solo lectura: si falla, falla esa pantalla y nada más.
- **C33/C35 cambian la aplicación de pagos en producción.** Son del cuadre («corrida definitiva», pedido del líder
  21-11); este EAR los lleva porque el usuario pidió todo `main`.
- Sin propiedades de sistema nuevas.
