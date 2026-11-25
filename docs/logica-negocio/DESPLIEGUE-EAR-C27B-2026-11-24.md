# Despliegue del EAR «c27b-1125b»: todo `main` para el próximo cierre de producción (2026-11-24)

> **Historial.** El primer EAR, `c27b-1125` (main `5fc3d9d2`), **NO se despliega**: usaba el alterno 38 para la
> plantilla del ⑪, y el 38 es `PlantillasCxp.COMPRAS`. Este documento es el del EAR corregido, `c27b-1125b`, que usa
> el alterno 39 (REGISTRO-RESERVAS §2c).

**omen-saa-1-arb.** Reemplaza a `c42-1124` y a `cancelados-1124`; ninguno de los dos llegó a desplegarse. Es el EAR con
el que producción hace el próximo cierre mensual con C27b activo, según la decisión D3 del líder.

## 1. El EAR
- `F:\ears\asoprep\c27b-1125\SaaEAR.ear`, SHA-256 `5FA3573628419C5A01FB492D78F3A4C40952D9BF632CE87D5459FF1925CBD452`
  (62.469.172 bytes).
  - 3 partes en `partes\`. Se unen con `copy /b SaaEAR.ear.part00+SaaEAR.ear.part01+SaaEAR.ear.part02 SaaEAR.ear`.
  - `testzip` sin errores.
- `saaBE.war` `FB25BA2D6ED465C5046F766B7C91C83D44A049B81A91E557F90FFB16E935F56E`.
  - Es saaBE `main` `e25adb79`, empaquetado con `mvn -o package` sobre un `git archive` limpio.
  - **Lleva el lote 5** (baja del bien por lo entregado, `POST /prdo/desmayorizar`). No lleva el USAP de app1.
- `SaaFE.war` `69B9403C…84276410`: saaFE `63a225d`. Desde ahí el FE solo cambió documentos.
- **Base y vuelta atrás:** `precancelar-vencido-1118`.

## 2. ⛔ ANTES del EAR, en este orden y bloque por bloque (cada script trae sus controles)
1. Los 9 scripts del cuadre del §2 de `DESPLIEGUE-EAR-CANCELADOS-2026-11-24.md`, si no corrieron. El bloque 6 de
   `crd/sql/382` lo dice.
2. `reportes/sql/C39-01`, luego `C40-01` (revisar el bloque 3 y hacer COMMIT), luego `C41-01`.
3. **`crd/sql/C45-03-D053-lineas-redondeo-4.8.99.08.sql` (OBLIGATORIO).** Sin él, el cierre de cartera (también el ④b)
   y la nómina se detienen con un mensaje.
4. `crd/sql/C45-02-D151-linea-61-devolucion-cobro-exceso.sql`.
5. `cuadre/def9-07-cuenta-4.8.90.90.45-perdidas-condonaciones.sql` (D-176) y
   `cuadre/def9-09-cuenta-4.8.99.9-gnd-multas.sql` (D-180).
6. **`crd/sql/383_C27B_INTERES_ANTICIPADO_PLANTILLA_39_Y_SUBPROCESOS_11_12.sql`.** Crea la plantilla alterno 39
   con la cuenta 2.9.01.05 (D2) y amplía `CK_ANCC_TPOO` a 1..12.
   - Su guarda lo detiene antes de escribir si el alterno 39 ya existe, o si 2.9.01.05 no existe o no es única.
   - No es re-ejecutable.
7. `cnt/sql/omen1-02-hcpr-accion-desmayorizar.sql` (lote 5). Sin él, `POST /prdo/desmayorizar` falla con ORA-02290 y
   se revierte.

## 3. Propiedades del servidor (C27b, ver `crd/C27b-PROPIEDADES.md`)

| Propiedad | Valor |
|---|---|
| `saa.cartera.devengoCompletoDesde` | El último día del primer mes que producción cierre con este EAR (2026-11-30 si es noviembre) |
| `saa.cartera.anclaArranque` | El `CRCTFCCR` de la corrida de arranque EJECUTADA (bloque 7 del `382`) |
| `saa.cartera.anticipadoBaseTransicion` | `TPPRCDGO:0` por cada tipo del bloque 8 del `382` (p. ej. `1:0;2:0;3:0;4:0;5:0`) |
| `saa.cartera.alCorte` | `siempre` (sin ella no corren ⑧, ⑨ ni ⑩) |
| `saa.cartera.corteInclusivo`, `saa.cartera.moraNoCobradaHasta` | **NO** se definen |
| `saa.cartera.timerMoraSuspendido` | No se define en producción |

## 4. Qué cambia en producción respecto de `precancelar-vencido-1118`
- Todo lo del §3 de `DESPLIEGUE-EAR-CANCELADOS-2026-11-24.md` y de `DESPLIEGUE-EAR-C42-2026-11-24.md`: el reporte de
  préstamos cancelados, C33, C35, C34, C42, C39, C40, C41, C43 y C44.
- **c45:**
  - el cobro duplicado se bloquea;
  - la precancelación directa en efectivo se rechaza;
  - el sobrante Petro devuelto va a 2.3.01.15.04;
  - el redondeo va a 4.8.99.08 (cartera y nómina);
  - los reversos se fechan con la fecha contable.
- **C27b (inerte hasta fijar `devengoCompletoDesde`):**
  - ④ con el interés completo;
  - ④b, la primera cuota;
  - ⑪, intereses recibidos por anticipado;
  - reversos de devengo en abono, precancelación (con aportes, con préstamo nuevo y por ventanilla) y acuerdo de
    condonación.
- **Lote 3:** la reapertura no revive novados ni reestructurados; la mora ya pagada no se recalcula; `desde` en
  `/calcularMora`; propiedad del timer.
- **Lote 4:**
  - EN_MORA con la fecha del proceso;
  - la reestructuración y la novación con su fecha;
  - la cuenta de banda por madre vigente al corte.
- **Lote 1c:** el asiento de la condonación queda en el evento y se anula al reversar; el acuerdo 100 % aportes va en un
  solo asiento y se reabre al anular.

## 5. Humo (solo lectura)
1. Verificar el hash y desplegar. En el log del arranque, sin `ERROR` ni `ORA-`. Revisar en especial la inyección
   circular entre `AcuerdoCondonacionServiceImpl` y `ProcesoPagoPrestamoServiceImpl`.
2. Correr el humo de `DESPLIEGUE-EAR-CANCELADOS` §4 y de `DESPLIEGUE-EAR-C42` §4.
3. **Cierre del mes (cuando toque):**
   - primero **previsualizar**;
   - en las advertencias, la línea del ⑪ (objetivo, asentado y base) y que aparezca el ④b;
   - recién entonces ejecutar, y correr el `384` sobre ese corte.
4. No hacer cobros ni asientos de prueba.

## 6. Riesgos
- **C27b nunca corrió contra Oracle.** El SQL del ④, del ④b y del ⑪ es nuevo. La previsualización del cierre es su
  primera ejecución real; si falla, falla el cierre antes de grabar nada.
- **Lote 3:** cambia el cálculo diario de la mora desde el primer disparo. La mora que el timer viejo ya recargó **no**
  se repara sola (`385`).
- **La parte `rhh` y `cxp` de c45** sigue pendiente de la revisión de su árbitro.
- **Restricción operativa de C27b:** volver a ejecutar un cierre ya ejecutado después de cancelaciones del mes siguiente
  puede duplicar el reverso. Antes se revisa con el `384`.
