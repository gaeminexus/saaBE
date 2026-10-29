# Traspaso — equipo omen-saa-1 (máquina omen, proyecto saa, equipo 1)

**Cierre ordenado: 2026-10-28.** Lo escribió el árbitro (omen-saa-1-arb). Este archivo es lo que hay
que leer primero para retomar. Lo que no está acá ni en un commit se perdió con la sesión.

## Alcance

- **Módulos del equipo:** `crd` (créditos) y `cnt` (contabilidad), en saaBE y saaFE.
- **NO TOCAR:** `cxp`, `cxc`, pagos, `tsr`, `rhh`, `sri`. Son de otros equipos (cxp/cxc es del
  equipo 2, omen-saa-2-arb; rhh es del equipo 3).
- **Roles:** árbitro `omen-saa-1-arb` (no escribe código, solo `.md`, `.sql` y prompts, nunca ejecuta
  SQL, es el único que commitea y pushea), ejecutores `omen-saa-1-be` y `omen-saa-1-fe`.
- **Tablero histórico del equipo:** `docs/logica-negocio/ESTADO-EQUIPO-OMEN-1.md`. Está
  **desactualizado desde el 2026-09-30**; este traspaso es más nuevo que el tablero.

## Terminado y verificado (en main)

| Commit | Repo | Qué |
|---|---|---|
| `7fd0b7bd` | saaBE | Desgravamen de **1,12 a 0,9 por mil** sobre el saldo de capital. Una sola constante, `CalculadoraAmortizacionServiceImpl.FACTOR_DESGRAVAMEN_SOBRE_SALDO`, para los simuladores (web y app), la generación real de la tabla y el abono a capital. Compiló con `mvn -q compile`. |
| `7fdb38e5` | saaBE | `docs/logica-negocio/cnt/sql/omen1-01-asientos-con-usuario-sistema.sql`: consulta de solo lectura de los asientos con usuario SISTEMA/SYSTEM/nulo. **El usuario todavía no la corre.** |
| `bc3468a` + `f63342a` | saaFE | Pantalla «Acceso App Móvil» en Créditos (pedido de omen-app-1-arb). Compiló con `ng build`. |

## A medias: el usuario SISTEMA en los asientos (PRIORIDAD ALTA)

**Problema (2026-10-26, el usuario lo calificó como «muy muy grave»):** hay asientos en producción
grabados con el usuario `SISTEMA`. Cuando el FE pierde el usuario de sesión (con F5 o en incógnito),
las pantallas no paran: mandan `'sistema'`/`'SYSTEM'` de relleno, y el BE graba `"SISTEMA"` si el
usuario llega nulo.

**Lo que pidió el usuario:** si en CUALQUIER momento el sistema detecta que perdió el usuario, sale y
obliga a loguearse; nunca graba un asiento ni corre un proceso sin usuario.

**Causa verificada en el código por el árbitro:**
- `saaFE src/app/shared/guard/auth.guard.ts`: solo mira `sessionStorage.logged === 'true'` y no
  verifica que exista el usuario.
- `saaFE src/app/shared/services/usuario-sesion.ts:23`: `usuarioSesion()` devuelve `'SYSTEM'` si no
  encuentra el usuario. Tiene unos 146 usos.
- `saaFE src/app/modules/cnt/forms/asientos-contables-dinamico/asientos-contables-dinamico.ts:810, 1026, 1610`:
  `localStorage.getItem('username') || 'sistema'`.
- `saaFE src/app/modules/dash/forms/login/login.component.ts`, `tryAutoRestoreFromSharedSession`
  (~140-180): si `localStorage.logged` es true y no hay username, igual entra.
- Pistas de por qué se pierde: `session-timeout.service.ts:507-512` y `app-state.service.ts:339-341`
  borran `logged` y `username`.
- `saaBE AsientoContableServiceImpl.java:508` (`usuario != null ? usuario : "SISTEMA"`) y
  `AsientoServiceImpl.java:604` (`marcaAuditoriaAnulacion`, rellena con "SISTEMA").

**Estado:** los dos despachos se mandaron el 2026-10-26 por SendMessage y **ningún ejecutor los
empezó**: no hubo cambios en disco ni respuesta, tampoco al pedido de entrega del cierre. Hay que
re-despacharlos completos. El contenido acordado es este:

- **FE (omen-saa-1-fe), ítems 1-8:**
  1. Diagnosticar primero por qué se pierde el usuario.
  2. `usuarioSesion()` sin relleno: si no hay usuario, cierra la sesión y lanza error.
  3. `authGuard` exige logged + usuario, con `canActivateChild` en los menús padre.
  4. Interceptor HTTP: un POST/PUT/PATCH/DELETE sin usuario se cancela, limpia la sesión, avisa
     «Tu sesión se perdió; vuelve a ingresar» y manda a /login. Excluye el login. También escucha los
     eventos `storage` y `visibilitychange`.
  5. El login no entra sin username.
  6. Quitar los rellenos solo en cnt/crd: `asientos-contables-dinamico.ts:810/1026/1610`,
     `generar-archivo-petro.component.ts:304`, `entidad-participe-info.component.ts:693`,
     `acciones-generacion-petro.service.ts:39`.
  7. Pruebas a mano: login + asiento; F5 con sesión válida; borrar el usuario en DevTools; incógnito.
  8. `ng build`.
- **BE (omen-saa-1-be), ítems 1-6:**
  1. Rastrear PRIMERO quién llama a la creación de asientos (`AsientoContableServiceImpl` ~508) y a la
     anulación, clasificando de dónde sale el usuario: del cliente, literal "SISTEMA" o puede llegar
     nulo.
  2. Si el usuario es nulo o vacío → `IncomeException`. Los procesos automáticos de crd/cnt pasan
     "SISTEMA" explícito.
  3. En el REST de cnt, rechazar con 400 `{"mensaje"}` un usuario vacío o `SISTEMA`/`SYSTEM` que
     venga del cliente.
  4. Reportar los procesos de crd que pierden el usuario.
  5. No corregir los datos ya grabados.
  6. `mvn -q compile`.

**Decisión hablada:** «SISTEMA» solo puede grabarse cuando un proceso automático lo pasa a propósito
(carga Petro, cobros, timers); nunca como relleno de un usuario nulo o vacío.

**Coordinación con el equipo 2 (autorizada por el usuario el 2026-10-26):**
- Se le avisó a omen-saa-2-arb y le pasamos los rellenos de cxc/cxp/tsr/rpr.
- El equipo 2 ya hizo su parte en cxp: `b1191051` (la bandeja graba el usuario real y rechaza sin
  usuario) y `8ead539f` (diseño del usuario de sesión en cxp).
- **Compromiso pendiente:** omen-saa-2-arb pidió que, ANTES de que el rechazo en cnt suba a main, le
  pasemos la lista de llamadores de sus módulos (cxc/cxp/tsr) que hoy mandan null. **El rechazo del BE
  no se commitea hasta que el equipo 2 confirme.**
- No está claro si tsr y rpr son del equipo 2; el usuario no lo confirmó.

**No es parte de este arreglo, pero el usuario ya lo sabe:** el backend no tiene autenticación real.
El login devuelve el texto `"OK"` como token y el backend cree el usuario que manda la pantalla.

## A medias: respuestas pendientes a omen-saa-2-arb

El 2026-10-27, omen-saa-2-arb mandó dos cosas:
1. Pide la lista de llamadores (ver arriba).
2. Pasó tres pedidos de Steven (contabilidad) que según ellos son de crd:
   - cierre de planillas: las cuentas 1.4.05.05, 2.3.02.05, 1.4.05.10 y 2.3.02.10 no quedan en 0
     (correo del 06-10);
   - la cuenta 2.3.01.10.03 con 35.789,20 (07-10);
   - el reporte financiero de crédito contra el balance (06-10).

   Su barrido está en el commit `e44b502d`. Si la cuenta puente sale de la confirmación de pagos,
   hay que avisarles.

**El usuario NO autorizó todavía la respuesta propuesta ni el diagnóstico de esos tres pedidos.**
Ninguno de los tres está diagnosticado.

## Hallazgo confirmado y no corregido: estados 12 y 13 en el motor de saldos

- `PrestamoDaoServiceImpl.selectVigentesByEntidad` (~164) y `PrestamoServiceImpl.calcularSaldosEnLote`
  (terminales ~1638) solo excluyen los estados 3, 4 y 5. No conocen el **12 ANULADO** ni el
  **13 CANCELADO_POR_REESTRUCTURACION**, y tampoco la cuota en estado 9.
- Efecto: un préstamo anulado o reestructurado puede contar como deuda vigente, y un reestructurado
  puede contar dos veces.
- Afecta a `DevolucionAporteRest`, que es nuestro, y a `/movil` de la app, que desde `4a65de40` lee
  el saldo de ese motor. La app ya filtra por su cuenta los anulados (estado 12, commit `e9205f73`)
  pero no los reestructurados (13).
- Lo reportó omen-app-1-arb y lo verificó el árbitro. El acuse ya se le mandó, diciendo que la
  corrección depende de la orden del usuario.
- **Pendiente: la decisión del usuario.** Antes de corregir, medir con un `.sql` si hay préstamos en
  estado 12 o 13 en producción.

## Decisiones habladas que no están en otro archivo

- **2026-10-23:** el desgravamen de 0,9 por mil aplica a TODO: simuladores, generación real y abono.
  El usuario avisó que **va a cambiar con el proceso de carga de seguros**. No hay que convertirlo en
  parámetro todavía; el diseño se hace cuando arranque ese proceso.
- **2026-10-23:** se le avisó a omen-app-1-arb que su simulador hereda el 0,9.
- **Compromiso con omen-app-1-arb:** avisarle ANTES de cambiar la firma o el resultado de
  `DetallePrestamoDaoService.selectByPrestamo`, `AporteDaoService.selectMovimientosPorEntidadYRangoDevengo`
  y `EstadoParticipeDaoService.selectAll`, porque `/movil` los usa.
- **Requisito antes del WAR:** el DDL `docs/logica-negocio/crd/sql/DDL-USUARIO-APP-MOVIL-RECUPERACION.sql`
  (columnas nuevas de CRD.USAP, mapeadas desde `ca8971a6`) va antes de cualquier WAR en una base que no
  sea producción. En producción ya está corrido.
- **El usuario confirmó que el 327 y el 324 ya corrieron en producción.**

## Roto o con riesgo

- **Asientos con usuario SISTEMA:** se siguen generando hasta que se haga el arreglo de arriba.
- **Estados 12 y 13:** el motor de saldos puede contar deuda que no existe.
- **Comentario desactualizado** en `SimulacionPrestamoServiceImpl` (~77-80): dice que la generación
  real usa un valor fijo de desgravamen, y eso es falso desde la decisión U1. No afecta el cálculo.

## No se empezó (backlog)

- **Tablero `ESTADO-EQUIPO-OMEN-1.md`:** ponerlo al día. Los scripts 306, 307, 311, 314, 316 y la
  secuencia 302 → 301 no tienen constancia de que se hayan corrido.
- **Decisiones abiertas de crd:**
  - impresos P1–P4;
  - RN10 y H85–H88;
  - retiro voluntario 21/22;
  - garantía en prendarios;
  - PV sin código (espera la salida del 328 y las cuentas del contador);
  - acuerdos K4/K5 y los 8 acuerdos del 320;
  - el Liberar de pólizas da 409.
- **Endpoints sin controles:** `/prst/pagarCuota`, `/prst/pagarMultiplesCuotas`, `/prst/precancelar`,
  `/pgpc/generarPagosDelMes`, `/asgn/procesarCargaPetro`, `/cfcr`.
- **Otros pendientes de crd:**
  - cobros personales en débito sin cobro ni asiento;
  - jubilados H46 y el motivo de bloqueo;
  - el timer de devoluciones está apagado;
  - mocks H72 y novedad-carga;
  - el verificador de esquema tiene 17 tablas faltantes.
- **cnt:**
  - Mayorizar desde Período da 404 (`/prdo/mayorizar` no existe).
  - La limpieza de temporales borra por PK (`eliminaConsultasAnteriores:384`).
  - La cabecera queda con `codigo 0L` en la distribución 2 (`MayorAnaliticoServiceImpl:336`).
  - Los balances salen sin RUC, sin la línea A = P + Pat y sin la marca de provisional.
  - Faltan los demás estados financieros.

## Pendiente del lado del usuario

- **Desplegar el WAR** con `7fd0b7bd` (desgravamen 0,9), si todavía no se hizo.
- **Correr `omen1-01`** y decidir qué hacer con los asientos ya grabados como SISTEMA.
- **Decidir:**
  - si se diagnostican los tres pedidos de Steven y se autoriza la respuesta a omen-saa-2-arb;
  - la corrección de los estados 12 y 13;
  - si tsr y rpr son del equipo 2.
- **Al re-despachar a los ejecutores:** verificar en su ventana que el mensaje se lea. El 2026-10-26
  los mensajes llegaron a la bandeja y nunca se procesaron; puede que haya que aprobarlos a mano o
  escribir en la sesión.

## Fuera del commit a propósito (de otros)

- `saaBE docs/logica-negocio/crd/modelos/`: sin seguimiento desde antes de esta sesión; no se sabe de
  quién es.
- `saaFE routes.json`: sin seguimiento; no es del equipo 1.
