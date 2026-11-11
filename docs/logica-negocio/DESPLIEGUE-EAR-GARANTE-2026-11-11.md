# Despliegue del EAR «pago de préstamo con aportes del garante» (2026-11-11)

**omen-saa-3-arb.** Se armó sobre `adhesion-1110` (omen1, `DESPLIEGUE-EAR-ADHESION-2026-11-10.md`), que contiene
`firmas-1110` y `retenida-1109`. Cambian los dos WAR. Es **todo `main`** de los dos repos al 2026-11-11.

## 1. El EAR
- `F:\ears\asoprep\garante-1111\SaaEAR.ear` — SHA-256 `16D49D922CD64948DA7F446C9B5FD38089BD3F60BE5CCC978E029CBA0BAA8485`
  (partido en `garante-1111\partes`, 3 partes; unir con
  `copy /b SaaEAR.ear.part00+SaaEAR.ear.part01+SaaEAR.ear.part02 SaaEAR.ear`).
  - `saaBE.war` `75668151930FC10B58E1937AE756DC524CD07FC1B65D0C47204F8133EF212CE2` — saaBE `main` en `d0bd20a4`
    (`mvn -o package` sobre un `git archive` limpio). Contra `adhesion-1110` cambian solo las clases de
    `ContextoPago`, `SolicitudPagoConAportes`, `GarantePrestamoServiceImpl`, `ProcesoPagoPrestamoServiceImpl` y
    `ContabilidadPrestamoServiceImpl` (más sus clases internas, por números de línea).
  - `SaaFE.war` `2E31BC3C450BEF3CC14F1D16CF91AEDC6EDFC7B2D61B26264BD62787B9823B86` — saaFE `main` en `c7d8e69`
    (`ng build --configuration production`; `WEB-INF` y `MANIFEST.MF` del SaaFE.war de `adhesion-1110`).
- **Vuelta atrás:** `F:\ears\asoprep\adhesion-1110\SaaEAR.ear` (`20FFAA7B…F361168F`). Sin scripts ni datos de por medio
  (los pagos con aportes del garante hechos con este EAR el viejo los anula igual: el reverso usa la entidad de la fila APRT).

## 2. Antes del EAR
- **Ningún script nuevo.** Los de `adhesion-1110` (376, 377) y el 374 ya estaban corridos.
- Propiedades de sistema: sin cambios.

## 3. Qué trae (además de `adhesion-1110`)
| Cambio | Commit |
|---|---|
| Pago de préstamo con aportes del garante: `idGarante` opcional en `POST /prst/pagarConAportes` (garante activo, del préstamo y partícipe; autorización firmada obligatoria); consume los aportes del garante y paga las cuotas del deudor; APRT/PGAP, evento, huella y asiento nombran al garante | BE `d0bd20a4` |
| Cruce de valores: «Pagar con: aportes del deudor / aportes del garante», saldos del garante, respaldo obligatorio, confirmación explícita, origen de los aportes en el resultado | FE `c7d8e69` |
| El garante es siempre partícipe: sin modo «tercero»; los históricos sin partícipe salen en solo lectura con «Anular y agregar otro» | BE `d0bd20a4`, FE `c7d8e69` |

Diseño: `crd/DISENO-PAGO-CON-APORTES-DEL-GARANTE.md`.

## 4. Despliegue y humo (solo lectura; sin pagos de prueba)
1. `Get-FileHash <ruta>\SaaEAR.ear -Algorithm SHA256` = `16D49D92…0BAA8485`. Desplegar. «Last enabled at»; log sin `ERROR`/`ORA-`.
2. Préstamo con garante: la pantalla del garante ya no ofrece «tercero».
3. Cruce de valores → deudor con préstamo con garante partícipe → aparece «Pagar con…»; al elegir el garante cambian
   los saldos. **No cruzar** salvo un caso real.
4. Primer caso real: revisarlo con el árbitro (APRT negativa en el garante, cuotas del deudor pagadas, asiento con la
   glosa del garante).

## 5. Riesgos
- Compila y arma; **nada corrió contra Oracle**. El cruce normal (sin garante) no cambia de código.
- El diálogo «Pagar con aportes» por fila (`pago-prestamo-dialog`) sigue solo con aportes del deudor.
- No hay aviso de «garante por debajo de su comprometido» (el FE no tiene ese dato sin reconstruir sus saldos).
