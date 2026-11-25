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
