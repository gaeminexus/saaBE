# C53 — Aportes del mes sin asiento y 2.1 contable contra el módulo (antes del cierre de cartera)

Equipo omen-saa-1 · 2026-11-28 · lote 32, punto 4.2 del encargo de controles de producción. Va a `ear/siguiente` por cherry-pick.

## 1. El caso
Re-proceso de julio 2026: dos REGISTRO APORTE de producción de una misma entidad (APRT 1026397 y 1026398, 161,47) sin asiento ni cobro. Solo los vio el semáforo `B17_21_VS_G42`, **después** de generar la G42. Hubo que quitarlos, reversar la corrida 266, ejecutar la 267 y regenerar las G (867), porque cambian la cuenta individual y la provisión del ⑧. Este lote los detecta **antes** del cierre de cartera.

## 2. Control A — `APORTES_SIN_ASIENTO`
### Cómo se vincula hoy un APRT con su asiento (medido en el código, no adivinado)
- `CRD.APRT` **no tiene columna de asiento**. El REGISTRO APORTE nace en `AporteServiceImpl.registrarAporte`: un `APRT` (`APRTTPMV = AJUSTE_MANUAL = 2`, glosa `REGISTRO APORTE <tipo>…`) y un `CRD.PGAP` (`PagoAporte`, `PGAPNMAS = null`, `PGAPIDST = 1`). **Ese método no genera asiento.**
- El asiento viene del **cobro** `CRD.CBCR` (`REGISTRO_APORTE` o `COBRO_MIXTO`): `CobroCreditoServiceImpl` llama a `registrarAporte` por cada línea y enlaza `CRD.DCBC.PGAPCDGO` → el `PGAP`; el cobro lleva el asiento definitivo en `CBCRASN2` (D 2.3.01.15.01 → H cuentas definitivas, entre ellas el pasivo del aporte).
- **Regla**: un aporte «tiene respaldo» si su `PGAP` vigente tiene `PGAPNMAS` (número de asiento), o es la línea (`DCBC.PGAPCDGO`) de un cobro cuyo `CBCRASN2` es un asiento vigente (estado 1 o 3). Sin ninguno: **sin asiento**.
### Qué aportes se miran
`CRD.APRT` vigente según `AporteVigenteSql` (sin el anulado por reproceso ni su contra), con `APRTTPMV = 2`, valor > 0 y `APRTFCTR` (fecha de caja) dentro del mes.
### Qué tipos se excluyen, y por qué
Los demás movimientos **no tienen un vínculo APRT → asiento por aporte**, porque se contabilizan en bloque: mensual (1), migrado (6), excedente Petro (8) y pago de préstamo (4) por la carga o el cobro Petro (asiento de aplicación, no por fila); devolución (3) por la devolución (`DVAP`); reverso (5) por el evento o la anulación; jubilación (7) y pago de pensión (9) por `PGPC`/pagos programados. Esos los cubre el **control B por saldo**: un aporte vigente de esos tipos sin asiento descuadra la 2.1 contra el módulo.

## 3. Control B — `APORTES_21_VS_MODULO`
- **Módulo**: la misma lectura que la G42, por tipo de aporte: `SUM(APRTVLRR)` neto de `CRD.APRT` vigente (`AporteVigenteSql`), con fecha de caja `< corte + 1`.
- **Contabilidad**: saldo acreedor (haber − debe) de la cuenta y sus hijas, asientos vigentes (1 y 3) de la empresa, al corte.
- **Cuenta por tipo**: `CRD.CTAP` vigente (`CTAPESTD = 1`) de la empresa → `CNT.PLNN.PLNNCNTA` (la misma lectura de la C47). Se compara **por cuenta** (suma de los tipos que la usan) y **en total 2.1** (los tipos cuya cuenta es la 2.1 o cuelga de ella, más los que no tienen fila en CTAP, como el Resumen Financiero). Los tipos con cuenta fuera de la 2.1 (p. ej. el seguro por entregar a beneficiarios) se comparan solo contra su cuenta.
- Tolerancia 0,005; **diferencia = contabilidad − módulo** (negativa: la contabilidad tiene MENOS que el módulo, p. ej. aportes sin asiento).

## 4. Dónde corre
| Dónde | Qué hace |
|---|---|
| `POST /cierrecartera/previsualizar` | advertencia «C53 …» con la lista (A, 15 aportes y el total) y las diferencias por cuenta (B) |
| `GET /prdo/controlCierre/…` | controles `APORTES_SIN_ASIENTO` y `APORTES_21_VS_MODULO` (grupo APORTES), **ERROR** si hay (el candado C51 los toma, excusables con motivo) |
| `POST /cierrecartera/ejecutar` | **no bloquea** salvo `saa.cartera.bloqueoAportesSinAsiento=true` (apagada por defecto; `IncomeException «APORTES_SIN_ASIENTO: …»`); no en la corrida de arranque |
La propiedad vive en `ControlAportesService`, no en `ModoCorteCartera`. Los controles corren en `REQUIRES_NEW`: una consulta que falle no marca rollback del cierre ni del semáforo (ADVERTENCIA «no se pudo medir»).

## 5. Archivos
- Nuevos: `crd/service/ControlAportesService.java`, `ReglaControlAportes.java`, `dto/ResultadoControlAportes.java`, `crd/serviceImpl/ControlAportesServiceImpl.java`, este doc, `src/test/java/com/saa/ejb/crd/service/ReglaControlAportesPruebas.java`, `tools/pruebas/correr-pruebas-c53.sh`.
- Modificados: `AporteVigenteSql` (pasa a `public` para que lo use el servicio; el contenido no cambia), `ControlCierreServiceImpl` (un `@EJB` y una línea, fuera de los hunks de la C19b), `CierreCarteraServiceImpl` (un `@EJB`, dos llamadas y dos métodos privados junto a los del C48).
- Sin DDL.

## 6. Aceptación del cuadre (copia de julio; NO corre aquí)
Restaurar `APRT 1026397` y `1026398` desde `CRD.BKP_APRT_JUL15B_5160_20261116`:
1. **A** los lista: 2 aportes, 161,47 en total (entidad 5160, tipo REGISTRO APORTE).
2. **B**: la 2.1 contable no cambia y el módulo sube 161,47 → diferencia contabilidad − módulo = **−161,47** (en la cuenta del tipo y en el total 2.1).
3. Con `saa.cartera.bloqueoAportesSinAsiento=true`, `POST /cierrecartera/ejecutar` de julio se rechaza con `APORTES_SIN_ASIENTO`; apagada, ejecuta y la previsualización lo muestra como advertencia.
4. Sin los dos aportes (estado actual de la copia), A: 0 y B: sin diferencias.

## 7. Compatibilidad con `origin/ear/siguiente`
`AporteVigenteSql` y `CierreCarteraServiceImpl` son idénticos en `main` y `ear/siguiente` (`git diff origin/main origin/ear/siguiente`); `ControlCierreServiceImpl` difiere en los hunks de la C19b (~45, ~510, ~546) y este lote no los toca. Depende de `CRD.PGAP`, `DCBC`, `CBCR`, `CTAP` y `CNT.ASNT/DTAS/PLNN`, que existen en las dos ramas.

## 8. No verificado
Nada corrió contra base ni WildFly. Las consultas usan columnas ya usadas por el repo (`APRTCDGO/ENTDCDGO/TPAPCDGO/VLRR/FCTR/TPMV`, `PGAPNMAS/IDST`, `DCBCPGAP`, `CBCRASN2`, `CTAPPLNP/ESTD`) pero no corrieron. Falta confirmar: (1) que `DCBC.PGAPCDGO` se llene también en los cobros históricos (si no, aportes de un cobro viejo saldrían como «sin asiento»; el control saldría ERROR de más); (2) que el REGISTRO APORTE de los cobros viejos tenga `APRTTPMV = 2`; (3) que la tolerancia de B coincida con lo que el cuadre espera por cuenta (la cuenta de pensión complementaria 2.1.02.25.01 y las demás de la 2.1 se comparan una a una: si una cuenta recibe asientos que no son aportes del módulo, saldrá diferencia).
