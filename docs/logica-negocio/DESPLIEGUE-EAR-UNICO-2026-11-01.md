# Despliegue del EAR único (2026-11-01)

**omen-saa-1-arb.** Reemplaza al EAR de cuentas a plantilla (`plantillas-1101`), que NO se desplegó.

## 1. El EAR
- `F:\ears\asoprep\unico-1101\SaaEAR.ear` — SHA-256 `B16F4A084BBD4C1216D6C05D20142769A52028B4ABD6D9C02D9B709ABB86A187`
  (copia partida en `unico-1101\partes`).
  - `saaBE.war` `CAD9F4E4E6551988C5E584BBEFBBA1450306E966396B88924F48A6D38459DFAC` — saaBE `main` en `5bae67db`.
  - `SaaFE.war` `3A7E0665D765507DC59850793A5E99895D11B209FDD6DDA87B9F9DE837CE8DBF` — saaFE `main` en `3e3c9dc`.
  - Armado sobre el EAR de correcciones desplegado (`correcciones-1101`, hash `41BDDCC3…`); solo cambian los dos WAR.
- **Vuelta atrás:** `F:\ears\asoprep\correcciones-1101\SaaEAR.ear`. Antes de volver atrás, desactivar el 1222 (§3, paso 1 a la inversa).

## 2. SQL previos — TODOS YA CORRIDOS en producción
`353` (verificación), `354` (PXCA.PRSTCDGO), `356` (plantilla 37 línea 85 y plantilla 38), `357` (CRD.DSSB y línea 61),
`361` (rubro 237: 517/516), `363` bloques 0-3 (rubro 237: 1222/976 y RHH.CFNM.CFNMPRPG = 345), `c20-02` (rubro 250:
16/17), `c20-04` (usuario REPROCESO 1562), `c20-06` (plantilla 38 línea 2). Más las correcciones de datos de
nómina (`rhh/omen1-04`, `omen1-05`) y NC de compra (`cxp/omen1-02`, `omen1-03`).

## 3. Despliegue (en este orden, SIN ninguna carga Petro/ARCH en proceso y fuera de horario de pagos)
1. **Activar el producto 1222** (bloque 4 del `363`) INMEDIATAMENTE ANTES de desplegar:
   ```sql
   UPDATE PGS.PRDP SET ESTADO = 1 WHERE ID = 1222 AND ESTADO = 0;
   COMMIT;
   ```
   ⚠ Entre este UPDATE y el despliegue, el código viejo no puede desembolsar préstamos (dos productos activos en
   2.3.90.90.10): hacerlo pegado al despliegue.
2. Desplegar `unico-1101\SaaEAR.ear`.
3. Verificar en el servidor: `Get-FileHash <ruta>\SaaEAR.ear -Algorithm SHA256` = `B16F4A08…ABB86A187`, y el log sin
   `ERROR`/`ORA-`/`ClassCastException` desde el despliegue (las coincidencias con «MarcadoErrorDocumento» son nombres de bean).

## 4. Qué cambia para los usuarios (avisar)
- **Todo proceso que graba exige un usuario real.** Sin sesión, la pantalla manda a ingresar de nuevo; nadie graba
  como «SISTEMA».
- **Si un asiento no se puede generar, la operación entera se revierte** y se ve el error (antes algunas anulaciones
  decían «éxito» con una advertencia).
- **Carga Petro/ARCH:** destino del sobrante por partícipe en el detalle de la carga; asientos a nombre de quien pulsa
  «Procesar».
- **Liquidaciones de excolaboradores:** el estado del pago se actualiza con el botón «Actualizar estado».
- **Nómina:** no se emite ni paga la orden sin el rol y las provisiones contabilizados.
- **NC de compra:** se contabiliza contra la factura que modifica (intermediario: a la cuenta de su producto).
- **Combos de producto de cxp:** muestran número y nombre de cuenta.

## 5. Pruebas
`PRUEBAS-EAR-UNICO-2026-11-01.md` — solo lectura o con operaciones reales que igual se iban a hacer; **nunca**
asientos de prueba.

## 6. Pendientes que NO van en este EAR
P10 (cuentas sugeridas de liquidación externa), P11 (cuenta definitiva del gasto sin clasificar: hoy 4.4.01.05
provisional), zona gris del pago externo sin desglose, resolutor de cxp de la devolución (equipo 2), «SAA_AH»,
reproceso que reaplique el destino del sobrante, eliminación de los cargadores históricos (cierre del proyecto).
