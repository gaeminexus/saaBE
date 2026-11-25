# Plan del encargo «contabilidad de producción correcta en todos los flujos de crédito» (24-nov-2026)

**omen-saa-1-arb.** El encargo lo pidió el líder del cuadre y lo trajo el usuario. Alcance de este equipo: `crd` y `cnt`.
Lo que cae en `rhh`, `cxp`, `tsr` o `pagos` (PGS) se marca **OTRO EQUIPO**: no se programa aquí; se avisa al árbitro
dueño, con la autorización del usuario.

## Estado por bloque

| Punto | Estado al 24-11 | Dueño | Próximo paso |
|---|---|---|---|
| 1.1 rama `cuadre-c39-c44-validaciones-g` | ✅ **Mergeada en `897d6eb4`.** Revisé crd y cnt. Mergea limpio y compila | omen1 | EAR `c42-1124` armado (ver `DESPLIEGUE-EAR-C42-2026-11-24.md`). DDL antes del WAR: C39-01 → C40-01 → C41-01 |
| 1.2 rama `cuadre/c19-balances-oficiales` | ⛔ **No existe en `origin`** | cuadre | Que la suban |
| 1.3 rama `cuadre/c45-bloqueantes-produccion` | ⛔ **No existe todavía** (la está programando el cuadre) | cuadre → revisión omen1 | Revisarla al llegar. ⚠️ 6.1 (nómina) y 6.2 (CxP) tocan `rhh`/`cxp`: esa parte la tiene que revisar su árbitro |
| 2.1 cobro duplicado | en c45 | cuadre | revisión |
| 2.2 tipo de aporte 12 | [DECIDE EL LÍDER CON EL CONTADOR] la cuenta | — | Semilla en la 21 cuando haya cuenta |
| 2.3 puentes 1.4.05/2.3.02 | diagnóstico | omen1 + cuadre | Pedirle al cuadre la lista de los ≈36 AJC de puente |
| 2.4 sobrante Petro devuelto | en c45 | cuadre | revisión |
| 2.5 EN_MORA contra hoy | pendiente | omen1-be | Lote 4 |
| 3.1 DTPRFCPG nulo | pendiente | omen1-be | **Lote 2** (después de C27b: mismos archivos) |
| 3.2 precancelación 100 % efectivo sin asiento | en c45 | cuadre | revisión |
| 3.3 condonación | (a) script def9-07 del cuadre; (b) y (c) [DECIDE EL LÍDER]; (d) va en C27b | — | (c) se propone con D-017 cuando decidan |
| 3.4 abono | dentro de C27b | omen1-be | Lote 1 |
| 3.5 reverso LIFO | diseño | omen1 | Diseño de una página antes de programar (cascada controlada) |
| 3.6 reapertura que revive novaciones | pendiente | omen1-be | Lote 3 |
| 3.7 reestructuración: fecha y pagaré | pendiente; las líneas 7.x [DECIDE EL LÍDER] | omen1-be | Lote 4 |
| 3.8 novación con `now()` | pendiente | omen1-be | Lote 4 |
| 3.9 bien en cuentas de orden | diseño (BE + FE: capturar avalúo) | omen1 | Lote 5 |
| 3.10 liquidación con deuda | propuesta de flujo | omen1 | Sin programar hasta aprobación |
| 3.11 castigo | anotado | — | — |
| 4.1 timer de mora | pendiente | omen1-be | Lote 3 |
| 4.2 devengo de mora ⑩ | verificación | usuario | Propiedades del bloque 7 |
| 5 C27b | **despachado al BE (lote 1)**, con la activación por propiedad (abajo) | omen1-be | ⑪ espera D2 |
| 6.1 redondeos | en c45 | cuadre | revisión (nómina = `rhh`, OTRO EQUIPO) |
| 6.2 fecha de hoy en contabilidad | en c45 | cuadre | revisión. CxP, tesorería y `rhh` = OTRO EQUIPO |
| 6.3 saldo contrario | pendiente | omen1-be (`cnt`) | Lote 4. Hace falta `cuentas-saldo-contrario.sql` del cuadre |
| 6.4 cierre del ejercicio | diseño BE + FE | omen1 | Lote 5 |
| 6.5 reabrir mayorizado | pendiente | omen1 | Lote 5 |
| 6.6 bandas al corte | pendiente | omen1-be | Lote 4 |
| 6.7 texto `sql/332` | trivial | omen1-be | Va con el lote 2 |
| 7 configuración de producción | SQL listo: `crd/sql/382_BLOQUE7_VERIFICACION_PRODUCCION.sql` + jboss-cli | usuario | Correrlo y pasarme la salida |

## Decisiones del líder del cuadre (24-nov-2026, noche)

- **D3:** C27b se activa en producción en el **próximo cierre mensual** que haga producción.
  - `devengoCompletoDesde` = último día de ese mes (2026-11-30 si es noviembre).
  - `anclaArranque` = la corrida de arranque EJECUTADA de producción (`crd/sql/382`, bloque 7).
  - `anticipadoBaseTransicion` = 0 en todos los tipos: producción nunca cargó el asiento manual C27b. Los TPPRCDGO se
    confirman con el bloque 8 del 382.
  - No hay regularización de lo acumulado en producción: esos meses los reemplaza la base re-procesada, con sus propias
    propiedades.
- **D1 y D5:** aprobados.
- **D2:** sigue pendiente del líder con el contador. El cuadre recomienda 2.9.01.05 compartida, que el diseño del ⑪
  soporta.
- **C45 (reversos con la fecha del aporte o de C17):** aprobado. Cambia lo que decía C17.
- **Orden de activación:**
  1. merge de c45 (la parte rhh/CxP la revisa su árbitro);
  2. lote 1b (precancelación por ventanilla y condonación);
  3. scripts 383 (con D2), C45-03 (obligatorio), C45-02, def9-07 y def9-09;
  4. 382 en producción, con `saa.cartera.alCorte=siempre`;
  5. propiedades y EAR;
  6. previsualizar el cierre (advertencias del ⑪, que aparezca ④b), ejecutar, y correr el 384.
- **Aparte:**
  - anular el asiento 8527 por REST y cargar el traslado de condonaciones (D-181);
  - revisar con Crédito el préstamo 8051 (evento 1 aplicado otra vez como evento 371, 204,44) y el 3425 (precancelación
    225 sin asiento).
- **Merge `cd539cf1`:** entró la rama `cuadre-docs-arbitro-1124`, que trae los documentos en `docs/logica-negocio/cuadre/`.

## C27b — decisiones del árbitro sobre la respuesta del cuadre

1. **Activación por propiedad `saa.cartera.devengoCompletoDesde` (AAAA-MM-DD, primer corte que se cierra con el código
   nuevo).** Sin la propiedad, ④ se comporta exactamente como hoy, y ④b y ⑪ no corren.
   - Por qué: todo va a `main` y `main` se despliega entero. Sin esto, cualquier EAR armado antes de la fecha fijada
     por D3 contabilizaría dos veces junto al asiento manual C27b del reproceso.
   - Así, la fecha de D3 «se fija por escrito» en la configuración del servidor.
2. **⑪ no se ajusta contra el saldo de la cuenta. Se ajusta contra lo que el propio ⑪ ya asentó**, más una base de
   transición:
   - lo asentado sale de la Σ de las líneas ⑪ de las corridas EJECUTADAS vigentes (`CRD.ANCC` subproceso 12 → asiento);
   - la base de transición es el objetivo al corte anterior a `devengoCompletoDesde`, que el asiento manual C27b ya
     dejó en la cuenta.
   - Ventajas: sirve con cuenta propia o compartida, así que D2 solo decide la cuenta de la semilla y no el código.
     Además, se corrige solo cuando un cobro anticipado se anula después del corte, cosa que el incremental
     «objetivo(t) − objetivo(t−1)» recalculado hoy no hace.
3. **`CK_ANCC_TPOO` pasa a 1..12** (④b = 11, ⑪ = 12), con script del pase.
4. **Prueba 8:** SQL de medición por caso borde, reproducible a fecha pasada, con su resultado esperado (D5).
5. **Medición del BE (24-11), decidida por el árbitro:**
   - **Ancla:** CRCT no marca la corrida de arranque. Se usa la propiedad `saa.cartera.anclaArranque`, validada contra
     una corrida EJECUTADA.
   - **Lo asentado por ⑪:** se lee de `CNT.DTAS`, lado 1.4.02.xx, de los asientos ANCC subproceso 12 activos. El tipo
     de préstamo sale de mapear cuenta → tipo con la plantilla.
   - **Universo por fecha:** entran los estados 9 y 10, el mismo universo que la G48. El ⑦ no cambia.
   - **Base de transición:** por propiedad, `saa.cartera.anticipadoBaseTransicion` = `TPPRCDGO:valor;…`, que da el
     cuadre igual a su asiento manual. Si falta, el cierre falla; no hay cero por defecto.
   - **Reversos en abono y precancelación:** solo para meses ya devengados por una corrida con
     corte ≥ `devengoCompletoDesde`, y dentro del asiento del evento.
   - **Condonación:** lo condonado ya va D gasto / H 1.4.02. Lo que se haga con ello es el punto 3.3(c).
6. **Lote 1 commiteado.** Lleva las correcciones del árbitro:
   - el reverso de la precancelación cuenta solo las cuotas que canceló ese evento y que están en meses devengados por
     el ④ nuevo;
   - el ④ y el ④b devengan solo lo cobrado de una cuota que ya está en estado 7, lo que cierra el hueco de una
     cancelación hecha entre el corte y el cierre;
   - lo ya asentado por ⑪ cuenta solo asientos ACTIVOS.
   - **Lote 1b, pendiente hasta el merge de c45:**
     - la precancelación por ventanilla (CBCRASN2) y la de 100 % efectivo no reversan el devengo. Con la propiedad
       activa, el hueco es la precancelación por ventanilla hecha después del cierre del mes. Es condición de
       activación (`C27b-PROPIEDADES.md`).
     - el acuerdo de condonación, por el hallazgo de abajo.
   - **Hallazgo, defecto previo:** reversar un cobro ACUERDO_CONDONACION (e4a69c65) deja vivo el asiento de la
     condonación.
     - `generarAsientoCondonacion` (l. 728-940) no guarda el asiento en el evento.
     - `reversarProceso` del CBCR anula solo sus tres asientos.
     - Se propone como lote aparte, junto con 3.3.
15. **D-187 (líder, 25-11): el EAR de producción para el cierre de noviembre es el código del 8080 del cuadre.**
    - Es `5a40b3ac` + lote 7 + lote 7b, **sin la C19b**. Se arma fuera del repo, sin rama, aplicando los commits de
      los lotes 7 y 7b sobre un `git archive` de `5a40b3ac`.
    - **La C19b va en el EAR siguiente,** con sus scripts y después de probarla en el reproceso: el B17 de mayo y el de
      septiembre 2025 tienen que salir idénticos byte a byte.
    - **Única excepción a «una sola versión»**, autorizada por el usuario y el líder.
    - **Lote 7b:** el tope de la provisión de mora va por PRÉSTAMO, compensando entre cuotas. El control 3 de la
      conciliación da 42.566,11 al 30-sep-2025, y el negativo por cuota (96 cuotas, −1.114,51) sale casi todo de la mora
      condonada en los acuerdos 25 y 26, el defecto 3.3(c). Rige hasta que se corrija 3.3(c).
14. **Verificado en producción (25-11):**
    - El alterno 39 está libre en todas las empresas.
    - El 38 es CXP COMPRAS (PLNSCDGO 1202, empresa 1236) y solo tiene las líneas aux1 1 y 2, sin 86/87. El 383 viejo
      nunca corrió en producción.
    - **El equipo `omen-saa-3` está apagado,** así que nadie más revisa la parte `rhh`/`cxp` de c45. La revisión que
      cuenta es la del árbitro omen1 del 24-11, sin hallazgos en esos dos archivos.
    - **EAR elegido por el usuario: opción (b),** con todo main más el lote 7 (lleva la c19b: `C19-01`…`C19-07` y el 392
      antes del WAR).
13. **«Merge de todo» (pedido del usuario, 24-11): merge `42050351` de `cuadre-c19b-sobre-main`.** Es el C19b del
    cuadre rebasado sobre main.
    - Borra `EstructuraReporteService` y porta C38/C39 al motor de balances. `/esrp` y `/esfo` quedan como alias.
    - **Requiere los SQL `rpr/sql/C19-01`…`C19-07` antes del WAR que lo lleve,** en el orden de `cuadre/README`.
    - **El EAR `c27b-1125c` NO lo lleva.**
    - **Las demás ramas no mergeadas ya están en main con otro hash** (`git cherry` sin commits pendientes): c11,
      c23 y las dos de lap1. La `c19-balances-oficiales` vieja queda reemplazada por la c19b.
    - saaFE no tiene ramas pendientes.
12. **Lote 5 commiteado (3.9 propuesta 1, 6.5 y medición de 6.4).** NO está en el EAR `c27b-1125`.
    - **3.9:** la baja del bien en cuentas de orden sale por lo que asentó la entrega.
    - **6.5:** `POST /prdo/desmayorizar`, con la bitácora HCPR DESMAYORIZAR. Requiere `cnt/sql/omen1-02` antes del WAR
      que lo lleve.
    - **Contratos para el FE:** `API-AVALUO-GARANTIA.md` (con lo [DECIDE EL CUADRE]) y `API-DESMAYORIZAR-PERIODO.md`.
    - **Medición:** 388 (residuo del bien) y 389 (alterno 1 duplicado, para 6.4).
    - **Pendiente:** la referencia colgante en `PRDOMYRZCIERRE` no se trata.
11. **Lote 1b commiteado y EAR `c27b-1125` (main `5fc3d9d2`).** C27b queda completo en código. Runbook
    `DESPLIEGUE-EAR-C27B-2026-11-24.md`.
10. **Lote 1c commiteado.**
    - El asiento de la condonación queda en el evento del acuerdo, así que reversar el cobro (o anular la operación) lo
      anula.
    - El acuerdo 100 % aportes va con un asiento único (condonación más cruce) y se reabre al anular su operación.
      - El reabrir es idempotente.
      - Con línea DCBC no hace nada: ese camino lo reabre el reverso del CBCR.
    - **Riesgo:** quedó una inyección circular entre `AcuerdoCondonacionServiceImpl` y `ProcesoPagoPrestamoServiceImpl`.
      Los dos son @Stateless con proxy, pero no se probó desplegado. Hay que mirarlo en el log del arranque.
    - **Producción:** el SQL 387 lista los asientos vivos y los acuerdos que siguen APLICADOS con el evento anulado.
      - La regularización (`REGULARIZACION-ASIENTOS-CONDONACION-1C.md`) espera la fecha de reverso del contador.
      - Reabrir los acuerdos del bloque 5 queda como lote aparte si se aprueba la lista.
9. **Lote 4 commiteado (2.5, 3.7a, 3.8, 6.6 parcial).**
   - **2.5:** EN_MORA se decide con la fecha del proceso en la reapertura, la prelación, la precancelación con préstamo
     y el reproceso Petro. El reverso sigue con hoy.
     - Queda fuera la rama PARCIAL de `recalcularCuotaDesdePagos`: allí la fecha no aplica.
   - **3.7a:** el asiento de la reestructuración va con `fechaOrdenPago`.
   - **3.8:** el evento de la novación o reestructuración va con la fecha de la operación (a las 00:00). El LIFO ordena
     por código, no por fecha.
   - **6.6:** `cuentaBandaMayorPorMadre` toma la configuración vigente al corte.
     - `gruposBandaCartera` no cambia hasta que el SQL 386 diga si difiere.
   - **6.3:** queda en dos pasos (control visible y luego bloqueante). Los PDTR del rubro 250 los reserva el árbitro.
   - **3.7b y 3.7c:** esperan las líneas 7.x y el merge de c45.
8. **Lote 1c (abierto por el usuario el 24-11): el asiento de la condonación queda vivo al reversar el cobro.**
   - **Objetivo:** reversar un cobro ACUERDO_CONDONACION anula también el asiento de `generarAsientoCondonacion`, con la
     misma fecha de reverso que usan los otros asientos del cobro.
   - **Camino preferido:** grabar el asiento en el evento del acuerdo (`EVPR.numeroAsiento`), para que
     `contabilizarReverso` lo anule por la vía de siempre, sin tocar los archivos de c45.
     - Antes hay que medir que el reverso del CBCR llegue de verdad a `contabilizarReverso` con ese evento.
     - Y que ningún otro flujo lea `numeroAsiento` de ese evento y cambie de comportamiento (por ejemplo, el rechazo
       U2 en `PrestamoRest.anularOperacion`).
   - **Si la vía obliga a tocar un archivo de c45,** el lote se hace después del merge de c45.
   - **Entregables:**
     - el cambio;
     - un SQL de medición de solo lectura con los acuerdos ya reversados cuyo asiento de condonación sigue activo, que
       es lo que hay que regularizar en producción;
     - el procedimiento de regularización, que va sin UPDATE y se aprueba antes.
   - Las líneas de reverso de devengo C27b de la condonación (lote 1b) van después, dentro de ese mismo asiento.
7. **Lote 3 commiteado (3.6 y 4.1).**
   - **3.6:** la reapertura excluye los estados 5 y 13 (en el servicio y en el predicado del DAO). Petro usa la misma
     regla. El dinero sigue la ruta de rechazo de cada proceso hasta que el cuadre decida su destino.
   - **4.1:** con mora pagada, mora = pagada + mora de los días desde max(vencimiento, `desde`, último cobro de mora).
     - Sin capital pendiente, la mora queda en lo pagado.
     - `desde` es opcional en `/calcularMora`.
     - `saa.cartera.timerMoraSuspendido=true` apaga el timer.
     - El SQL 385 mide la mora recargada por el timer viejo, que **no** se repara sola.
     - Hallazgo: la base de los días nuevos es el capital de la cuota (DTPRCPTL), salvo con
       `moraSobreCapitalPendiente`. Es la convención existente y no se cambió.

## Lotes del BE (uno a la vez; no se despacha el siguiente sin commitear el anterior)

1. **C27b:** ④ corregido, ④b, ⑪ con la plantilla parametrizada, los reversos en abono, precancelación y condonación,
   la propiedad de activación, CK_ANCC y el SQL de medición.
2. 3.1 DTPRFCPG y 6.7.
3. 3.6 reapertura y 4.1 timer de mora.
4. 2.5, 3.7 (fecha y pagaré), 3.8, 6.3 y 6.6.
5. 3.9, 6.4 y 6.5 (con FE).
