# Traspaso — equipo omen-saa-1 (máquina omen, proyecto saa, equipo 1)

# ESTADO VIGENTE AL 2026-11-01 04:30 (léase primero; lo de más abajo es historia)

**Producción corre el EAR único** (saaBE `5bae67db`, saaFE `3e3c9dc`), desplegado a las 04:28 con el hash
verificado y el log limpio: `docs/logica-negocio/DESPLIEGUE-EAR-UNICO-2026-11-01.md`.

**Alcance actual del equipo 1** (ampliado por el usuario el 31-10/01-11): crd, cnt, cxp, cxc, pagos, tsr, rhh, sri y
asoprep. Los equipos 2 y 3 están abajo; sus avisos están al final de este archivo.

## Hecho y en producción
| Frente | Documento rector |
|---|---|
| Carga Petro/ARCH: B1 sin mora sobre lo descontado, B2 adelanto y AVPC a aporte por mes, B3 aportes por carga real, B4 ARCH (.csv, parcial bloqueante, cédula, préstamo exacto), B5 seguro de incendio, B7 limpieza | `petro/PLAN-CORRECCIONES-CARGA-2026-11-01.md` |
| B8 destino del sobrante por partícipe (CRD.DSSB) y devolución a socios por pagar con orden en Tesorería (producto 1222) | `petro/DISENO-DESTINO-SOBRANTE-DSSB.md` |
| B6-a/b asientos de la carga a nombre de quien pulsa «Procesar»; reproceso con REPROCESO (≤2026-08) o el usuario de la carga | `cnt/DISENO-USUARIO-OBLIGATORIO.md` §8 |
| Cuentas fuera del código: P1-P5, P8, P11, P12; P9/P9b y producto de pago por parámetro (rubro 237: 517/516/1222/976; RHH.CFNM.CFNMPRPG = 345) | `cnt/PLAN-CUENTAS-A-PLANTILLA-2026-11-01.md` |
| N1 nómina por empleado (producto desde la línea 50); datos de octubre corregidos | `rhh/RECLASIFICACION-NOMINA-OCTUBRE-2026.md` |
| N2 NC de compra contra la factura que modifica (intermediario sin IVA); las 3 NC mal contabilizadas, corregidas por UPDATE | `cxp/DISENO-FACTURA-INTERMEDIARIO.md` §8, `cxp/sql/omen1-02` y `omen1-03` |
| Usuario obligatorio en TODOS los módulos (U-2 a U-4) y defensa central atómica (U-5): sin usuario real no se graba nada y un asiento rechazado revierte el proceso entero | `cnt/DISENO-USUARIO-OBLIGATORIO.md`, `cnt/INVENTARIO-USUARIO-OBLIGATORIO.md`, `cnt/INVENTARIO-ATOMICIDAD-ASIENTOS.md` |
| FE: F1-F12 (sesión obligatoria, ARCH, destino del sobrante, número de cuenta en los combos de cxp, usuario en todos los servicios, «Actualizar estado» en liquidaciones externas) | contratos en `saaFE/docs/{crd,cxc,cxp,tsr,rrh}/API-*.md` |
| Integración del C11 del cuadre (`6a43d585`) | — |

**Esto resuelve los pendientes de la historia de abajo:** asientos con «SISTEMA» (U-2 a U-5); `/prst/pagarCuota` y
`/pagarMultiplesCuotas` cerrados (410); estados 12 y 13 medidos con `crd/sql/350` en producción: vacíos, así que no hay
impacto hoy (la corrección preventiva sigue pendiente).

## Pendiente — decisión del usuario
- **Pago de origen externo sin desglose** confirmado sin asiento (`PagoProgramadoServiceImpl` ~3260): ¿se bloquea?
  Recomendado: bloquear, salvo cuando el origen tiene la contabilidad apagada a propósito.
- **P11:** cuenta definitiva del gasto sin clasificar de compras (hoy 4.4.01.05 provisional, plantilla 38 línea 2).
- **P10:** cuenta sugerida por concepto en la liquidación de excolaboradores (conceptos 1-4 tienen dos candidatas).
  La propuesta del BE (plantilla por concepto + endpoint) está en el traspaso del 01-11.
- **Cuenta del producto 1222** (devolución): hoy en el mismo grupo que el desembolso (2.3.90.90.10), hasta que
  contabilidad confirme.

## Pendiente — técnico (sin prisa)
- Resolutor de seguimiento de cxp para `CRD_DEVOLUCION_DESCUENTO` (equipo 2).
- «SAA_AH» como usuario de los aportes de la carga: hay SQL de diagnóstico que filtra por él.
- El reproceso no vuelve a aplicar el destino del sobrante (no afecta al reproceso definitivo: ninguna carga histórica tiene DSSB).
- Endpoint «autorizados sin asiento» en cxc (opcional).
- Mora sobre capital ya pagado (ME/M1), aparte, por decisión del usuario.
- Corrección preventiva de los estados 12 y 13 en el motor de saldos.
- Los 400 de `/extr/selectByCriteria` del 01-11 a las 00:51 (otra pantalla; no es de este equipo).
- Al cierre del proyecto: eliminar los cargadores históricos C11/C13 (ver abajo).

## Pendiente — coordinación con el reproceso (árbitro del cuadre)
- Mensaje a pasar: los 64 pagos `PAGO_MANUAL` sin cobro del 14 al 26-08 (4.755,37) y los 33 de hora 00:00 de
  jun-jul (11.581,43) no tienen CBCR; si el reproceso solo rehace cobros, quedarían sin asiento. Script:
  `crd/sql/VERIFICACION-PAGOS-MANUALES-SIN-ASIENTO.sql`. Los 6 cobros sin asiento del 31-07 (contabilidad
  apagada) se contabilizan al rehacer el cobro.

## Lecciones del 01-11
- **Un producto de pago nuevo en una cuenta que ya tiene uno se crea INACTIVO** y se activa con el EAR que lo usa
  por parámetro. Se corrió activo por error y detuvo los desembolsos hasta desactivarlo (`crd/sql/362`).
- **El asiento de un pago de caja vive en el cobro (DCBC/CBCR), no en el evento del préstamo**: un EVPR/PGPR sin
  asiento propio no prueba nada.

---

# HISTORIA (anterior al 2026-11-01)

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

---

# Jornada del 2026-10-31 (sesión reanudada, alcance ampliado: crd, cnt, cxp, cxc, pagos, tsr, rhh, sri, y asoprep autorizado)

## Producción
- **Ambientes:** producción = `F:\ears\asoprep\produccion\SaaEAR.ear` (GitHub `main`); «cuadre de
  producción» = `F:\ears\asoprep\SaaEAR.ear` (reproceso de la laptop). No confundirlos.
- **Desplegado esta noche:** `F:\ears\asoprep\arch-v2\SaaEAR.ear` = `main` `8ac1d97a` (saaBE) +
  saaFE `080e7da`. Trae A1 (AVPC en AH/HS huérfano), la carga ARCH, el orden de meses por filial y el
  recorte del nombre a 35.
- **Sin desplegar (EAR v3, pendiente de armar):** saaBE `c6718e24` (glosa «Mes X (pagado en Y)») y
  saaFE `ec496e8` + `47d1cf1` (columna «Mes del aporte» y fecha de caja en el ojo).

## Cargas de octubre: las dos procesadas antes del 01-11
- **Petro 492:** procesada tras A1. Las AVPC 688-692 a cesantía quedaron en una fila sin mes; se
  partieron por mes con `crd/sql/337` y la glosa se corrigió con el `338`.
- **ARCH 510:** primera respuesta de ARCH cargada por el sistema. Abortó la primera vez por 0,10: el
  aporte migrado 267360 tenía `APRTIDAS = 510`. Se corrigió con el `341`, y el `342` borró `APRTIDAS`
  de los ~2 M aportes migrados (BATCH sin carga; respaldo en `CRD.BK_APRT_APRTIDAS_20261031`). Al
  correr el `342` en paralelo con la carga, la base se saturó (free buffer waits). **Nunca correr un
  UPDATE masivo con una carga en proceso.** Aportes corregidos con el `343` y el `345`: junio y julio
  de `SAA_JUL_FIN` no tenían devengo y contaban los dos como julio, por eso octubre cayó en junio.

## Hallazgos (con su documento)
- `petro/DISENO-AFECTACION-MANUAL-V2.md`: F1-F9, H1-H6, P1-P8; §4bis (A1); §6 (glosa y mes del aporte).
- `petro/DISENO-CARGA-TARDIA-SIN-MORA.md`: la carga cobra la mora guardada antes que el capital. Si se
  procesa después de las 02:00 del día 1, deja cuotas PARCIALES. Decisiones MA a ME del usuario.
- `petro/DISENO-CARGA-RESPUESTA-ARCH.md` §9 y §9.5: carga ARCH implementada. Pendientes: .csv no
  permitido en FileService, parcial no siempre bloqueante (A6), PE con atraso marcado inconsistente
  (A4), no usa el préstamo de la línea PDGA (A5), normalización de cédula (A8).
- `REGLAS-CARGA-PETRO.md`: anticipo de aportes roto (esperado en lote solo hasta el mes de la
  carga); aportes de la carga contados por `APRTIDAS` (los migrados chocaban).
- La afectación manual a aporte graba una fila sin mes: pendiente repartir por mes, junto con el
  arreglo del anticipo.

## Pendiente (orden sugerido)
1. Armar y desplegar el EAR v3.
2. Revisar las cargas 448, 449, 450 y 492, que mandaron aportes a junio 2026 (CONTROL 2b del `345`):
   ¿legítimo o corrimiento?
3. Código: AVPC a aporte por mes + anticipo; control y asiento por carga real (no por `APRTIDAS`);
   mora en la carga tardía (decisiones MA a ME); pendientes de ARCH (A4-A8); procesar meses atrasados
   de una filial.
4. Unificación con el cuadre: `origin/cuadre` saaBE `9ecdd2f2` (ya fusionado con `main` hasta
   `bb36d423` por un pull en la laptop; hay que revisar los merges `36e5867b` y `f8b0a809`) y saaFE
   `9f1cc4b` (le falta `080e7da`). Choques de numeración de sql: 332, 335, 336 y 338 (renumerar los
   del cuadre). Plan de pase con ensayo sobre un dump; decisiones I-3, `moraNoDevengadaAIngreso`,
   c12-01, permisos, IGFNCMNM y firmantes de C19-04.
5. Diseño de la carga ARCH con el PDF: las respuestas de junio a septiembre se registraron por
   cobros personales (agosto y septiembre con devengo correcto).

---

# AVISO PARA LOS EQUIPOS 2 Y 3 AL RETOMAR (escrito por omen-saa-1-arb el 2026-11-01; estaban abajo)

1. **`main` es la versión unificada:** se fusionó la rama `cuadre` (saaBE `2428217a`, saaFE `72ea63b`) y
   se desplegó en producción el 01-11 (`docs/logica-negocio/PASE-PRODUCCION-FUSION-2026-11-01.md`).
   Antes de trabajar, `git pull`.
2. **Todo WAR de `main` desde `0926d8bb` necesita `crd/sql/354_DDL_PXCA_PRESTAMO_DE_LA_LINEA.sql` corrido
   antes** (columna `CRD.PXCA.PRSTCDGO`). En producción ya está corrido.
3. **Usuario de sesión (saaFE `6e4e92d`):** `usuarioSesion()` ya no rellena con `'SYSTEM'`; sin usuario,
   cierra la sesión y manda a /login. Un interceptor corta todo POST/PUT/PATCH/DELETE sin usuario. Afecta
   a TODOS los módulos: cxc (9 usos), cxp (2), tsr (4), rrh (64). Si una pantalla suya grababa con
   'SYSTEM' cuando faltaba el usuario, ahora pide volver a ingresar.
   **Pendiente compartido con el equipo 2:** `app-state.service.ts` → `getIdUsuario()` cae a 0
   (`|| 0`); lo usan anulaciones de cxc/cxp. Hay que acordar el cambio a `null`.
4. **Bancos reales:** el BLOQUE 2 del `e2-111` se corrió en el pase (6 bancos en 0 y 12 en 1).
5. **Regla del usuario:** ninguna cuenta contable escrita en el código; todas por plantilla.

## Pendiente del cierre del proyecto (decisión del usuario, 2026-11-01)
- **Eliminar los cargadores históricos C11 y C13** después de que el reproceso definitivo llegue a
  producción. Hoy son la única vía del reproceso para jun-2025 → jul-2026 (respuesta del árbitro del
  cuadre, D-012/D-013).
- Qué eliminar:
  - backend: `ejb/historico`, `model/historico` y `ws/rest/historico`;
  - frontend: nada, porque no hay ninguna llamada a `/historico`.
  - Ojo: `esHistorico()` de nómina y `cargarHistoricoCierres()` de tsr son otra cosa; no se tocan.
- Mientras tanto, opcional: desactivar el REST `/historico` en producción, conservando el código.

## AVISO para los equipos 2 y 3: usuario obligatorio en sus módulos (2026-11-01)
Decisión del usuario: «Asientos sin usuario se deben rechazar y el proceso que los ejecute no se debe
realizar […] lo primero que se debe validar en los procesos es si el usuario existe».
- Con los equipos 2 y 3 abajo y el usuario pidiendo todo en un solo EAR, el equipo 1 hace **U-3 (cxc/cxp)**
  y **U-4 (tsr, rhh, pagos y sri)** en sus módulos:
  - cada proceso que graba valida primero el usuario con
    `com.saa.basico.ejb.UsuarioValidacionService` (existe en `SCP.PJRQ`);
  - se quitan los rellenos `x != null ? x : "SISTEMA"`;
  - la recepción del SRI en cxp graba con el usuario de quien la ejecuta.
- Inventario: `docs/logica-negocio/cnt/INVENTARIO-USUARIO-OBLIGATORIO.md`. Diseño:
  `cnt/DISENO-USUARIO-OBLIGATORIO.md`.
- Contratos del FE en `saaFE/docs/{crd,cxc,cxp,...}/API-USUARIO-OBLIGATORIO-*.md`.
- **Al retomar:** `git pull` y revisar los commits `cxp(omen1)`, `cxc(omen1)`, `tsr(omen1)` y `rhh(omen1)` en
  sus archivos.
- **Equipo 3, FE rrh:** la tabla compartida `rrh/forms/comunes/tabla-rrh` ganó una acción por fila OPCIONAL
  (`@Input accionExtra` / `@Output accionExtraClick`, tipo `AccionFilaTabla` en `modelo-formulario.ts`; saaFE `F12`). Las
  pantallas que no la usan no cambian. La lista de liquidaciones de excolaboradores ya no sincroniza sola con
  Tesorería: botón «Actualizar estado».

## 2026-11-01 04:28 — EAR único desplegado en producción (saaBE 5bae67db, saaFE 3e3c9dc)
Trae B1-B8 (carga Petro/ARCH, destino del sobrante y devolución), B6-a/b (asientos a nombre de quien procesa), cuentas a
plantilla (P1-P5, P8, P9/P9b, P11, P12 y producto de pago por parámetro), N1 (nómina individualizada), N2 (NC de compra),
y el usuario obligatorio con defensa central atómica (U-2 a U-5) en todos los módulos.
Runbook y resultado: `docs/logica-negocio/DESPLIEGUE-EAR-UNICO-2026-11-01.md`.
